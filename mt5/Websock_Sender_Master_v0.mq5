//+------------------------------------------------------------------+
//| MasterSignalSenderWS.mq5                                         |
//| Master side: capture deals & order events and send JSON via WS   |
//| WebSocket based on MetaQuotes shared project (Socket/TLS)        |
//+------------------------------------------------------------------+
#property strict
#property version "1.00"

// 你的工程只有 Headers，就从 Headers 引入 wsclient
#include "wsclient.mqh"
//#include <stdlib.mqh> // ErrorDescription

//----------------------- Inputs ------------------------------------
input string WsUrl               = "ws://localhost:8080/ws/trade";
input bool   UseCompression       = false;

input string SubProtocol          = "";     // 可选：Sec-WebSocket-Protocol
input string BearerToken          = "";     // 可选：Authorization: Bearer xxx
input string Mapping_string       = "";     // 预留：目前不使用

input int    TimeoutMs            = 5000;
input int    TimerPeriodMs        = 200;    // WS pump + flush 周期
input int    ReconnectIntervalMs  = 1500;
input int    HeartbeatIntervalMs  = 3000;

input int    MaxOutbox            = 2000;   // 断线缓存
input int    MaxDealRetries       = 50;     // 等历史落盘的重试次数
input int    MaxOrderRetries      = 10;
input int    SendBatchPerTick     = 200;    // 每次 timer 最多发送条数

//----------------------- JSON helpers ------------------------------
string JsonEscape(const string s)
{
   string out="";
   for(int i=0;i<StringLen(s);i++)
   {
      ushort c=StringGetCharacter(s,i);
      if(c=='\\') out+="\\\\";
      else if(c=='"') out+="\\\"";
      else if(c=='\r') out+="\\r";
      else if(c=='\n') out+="\\n";
      else if(c=='\t') out+="\\t";
      else out+=CharToString((uchar)c);
   }
   return out;
}

string BuildRequestTarget(const string address, const string parsed_path)
{
   string target = parsed_path;
   if(target == NULL || target == "")
      target = "/";

   if(StringFind(target, "?") >= 0)
      return target;

   int query_pos = StringFind(address, "?");
   if(query_pos < 0)
      return target;

   string suffix = StringSubstr(address, query_pos);
   if(suffix == NULL || suffix == "")
      return target;

   return target + suffix;
}

//----------------------- WebSocket client wrapper -------------------
// 说明：你提供的 wsclient.mqh 中 url=address，会把完整 ws://... 传给 handshake 的 GET 行。
// 为了不改你的文件，这里通过派生类修正：
// 1) handshake 的 url 仅使用 URL_PATH
// 2) Host header 使用 host:port（非默认端口）
class CMasterWsClient : public WebSocketClient<WebSocketConnectionHybi>
{
private:
   string m_host_socket;
   uint   m_port_num;
   string m_host_header;

public:
   CMasterWsClient(const string address, const bool useCompression)
      : WebSocketClient<WebSocketConnectionHybi>(address, useCompression)
   {
      string parts[];
      URL::parse(address, parts);

      // scheme/host/port/origin/url 是 wsclient 的 protected 成员，可在派生类里覆写
      scheme = parts[URL_SCHEME];
      if(scheme != "ws" && scheme != "wss") scheme = "ws";

      m_host_socket = parts[URL_HOST];
      string port_s = parts[URL_PORT];
      m_port_num = (uint)StringToInteger(port_s);
      if(m_port_num == 0)
         m_port_num = (scheme == "wss" ? 443 : 80);

      // 使用 path + query 作为 GET target，确保 access_token 这类查询参数不会在握手时丢失
      url = BuildRequestTarget(address, parts[URL_PATH]);

      // host 用于 SocketConnect（不能带 :port）
      host = m_host_socket;
      port = (string)m_port_num;

      bool defaultPort = (scheme=="ws"  && m_port_num==80) || (scheme=="wss" && m_port_num==443);
      m_host_header = defaultPort ? m_host_socket : (m_host_socket + ":" + (string)m_port_num);

      origin = (scheme=="wss" ? "https://" : "http://") + m_host_header;
   }

   // 覆写 open：socket 仍用 host + port；handshake 的 Host header 用 host:port
   bool open(const string custom_headers = NULL)
   {
      if(socket) { delete socket; socket = NULL; }
      if(connection) { delete connection; connection = NULL; }

      socket = MqlWebSocketTransport::create(scheme, host, (uint)StringToInteger(port), (uint)timeOut);
      if(!socket || !socket.isConnected())
         return false;

      connection = new WebSocketConnectionHybi(&this, socket, compression);
      return connection.handshake(url, m_host_header, origin, custom_headers);
   }

   // 发送端不处理业务消息，避免堆积：收到就释放
   void onMessage(IWebSocketMessage *msg) override
   {
      delete msg;
   }
};

//----------------------- Globals -----------------------------------
CMasterWsClient *g_ws = NULL;
ulong g_last_connect_try = 0;
ulong g_last_heartbeat   = 0;

// outbox
string g_outbox[];

// pending deals (wait history)
struct PendingDeal
{
   ulong    deal_ticket;
   datetime enqueue_time;
   int      tries;
};
PendingDeal g_pending_deals[];

// pending orders (sometimes need a few retries for history select)
struct PendingOrder
{
   int      trans_type;
   ulong    order_ticket;
   ulong    position_ticket;
   datetime enqueue_time;
   int      tries;
};
PendingOrder g_pending_orders[];

//----------------------- Queue utilities ---------------------------
void OutboxPush(const string json)
{
   if(json=="") return;

   int n=ArraySize(g_outbox);
   if(n>=MaxOutbox)
   {
      for(int i=1;i<n;i++) g_outbox[i-1]=g_outbox[i];
      g_outbox[n-1]=json;
      return;
   }
   ArrayResize(g_outbox, n+1);
   g_outbox[n]=json;
}

void OutboxRemoveAt(const int i)
{
   int n=ArraySize(g_outbox);
   if(n==0 || i<0 || i>=n) return;
   if(i!=n-1) g_outbox[i]=g_outbox[n-1];
   ArrayResize(g_outbox, n-1);
}

void PendingDealPush(const ulong deal_ticket)
{
   PendingDeal pd;
   pd.deal_ticket  = deal_ticket;
   pd.enqueue_time = TimeCurrent();
   pd.tries        = 0;

   int n=ArraySize(g_pending_deals);
   ArrayResize(g_pending_deals, n+1);
   g_pending_deals[n]=pd;
}

void PendingDealRemoveAt(const int i)
{
   int n=ArraySize(g_pending_deals);
   if(n==0 || i<0 || i>=n) return;
   if(i!=n-1) g_pending_deals[i]=g_pending_deals[n-1];
   ArrayResize(g_pending_deals, n-1);
}

void PendingOrderPush(const int trans_type, const ulong order_ticket, const ulong position_ticket)
{
   PendingOrder po;
   po.trans_type   = trans_type;
   po.order_ticket = order_ticket;
    po.position_ticket = position_ticket;
   po.enqueue_time = TimeCurrent();
   po.tries        = 0;

   int n=ArraySize(g_pending_orders);
   ArrayResize(g_pending_orders, n+1);
   g_pending_orders[n]=po;
}

void PendingOrderRemoveAt(const int i)
{
   int n=ArraySize(g_pending_orders);
   if(n==0 || i<0 || i>=n) return;
   if(i!=n-1) g_pending_orders[i]=g_pending_orders[n-1];
   ArrayResize(g_pending_orders, n-1);
}

//----------------------- WS connect / pump --------------------------
string BuildCustomHeaders()
{
   string hdr = "";
   if(SubProtocol!="")
      hdr += "Sec-WebSocket-Protocol: " + SubProtocol + "\r\n";
   if(BearerToken!="")
      hdr += "Authorization: Bearer " + BearerToken + "\r\n";
   return hdr;
}

bool WS_Connect()
{
   if(g_ws && g_ws.isConnected())
      return true;

   ulong now = GetTickCount();
   if(now - g_last_connect_try < (ulong)ReconnectIntervalMs)
      return false;
   g_last_connect_try = now;

   if(g_ws)
   {
      g_ws.close();
      delete g_ws;
      g_ws = NULL;
   }

   g_ws = new CMasterWsClient(WsUrl, UseCompression);
   g_ws.setTimeOut(TimeoutMs);

   string hdr = BuildCustomHeaders();
   if(!g_ws.open(hdr))
   {
      int err = GetLastError();
      PrintFormat("WS open failed: %s err=%d %s", WsUrl, err, ErrText(err));
   
      // 先硬编码你当前目标，确保能打出真实 SocketConnect 错误
      DumpSocketDiagnostics("127.0.0.1", 8080, (uint)TimeoutMs);
   
      delete g_ws;
      g_ws = NULL;
      return false;
   }


   // HELLO
   long login=(long)AccountInfoInteger(ACCOUNT_LOGIN);
   string srv=AccountInfoString(ACCOUNT_SERVER);
   PrintFormat("WS connected. target=%s account=%I64d server=%s bearer_len=%d",
               WsUrl, login, srv, StringLen(BearerToken));
   string hello=StringFormat(
      "{\"type\":\"HELLO\",\"login\":%I64d,\"server\":\"%s\",\"ts\":\"%s\"%s,\"positions\":[%s]}",
      login,
      JsonEscape(srv),
      TimeToString(TimeCurrent(), TIME_DATE|TIME_SECONDS),
      BuildAccountFundsJson(),
      BuildHeldPositionsJson()
   );
   g_ws.send(hello);

   return true;
}

void WS_Pump()
{
   if(!g_ws) return;
   g_ws.checkMessages(false); // non-blocking
}

void WS_SendHeartbeat()
{
   ulong now = GetTickCount();
   if(now - g_last_heartbeat < (ulong)HeartbeatIntervalMs)
      return;
   g_last_heartbeat = now;

   if(!WS_Connect()) return;

   string hb=StringFormat(
      "{\"type\":\"HEARTBEAT\",\"ts\":\"%s\"%s,\"positions\":[%s]}",
      TimeToString(TimeCurrent(), TIME_DATE|TIME_SECONDS),
      BuildAccountFundsJson(),
      BuildHeldPositionsJson()
   );
   if(!g_ws.send(hb))
   {
      g_ws.close();
      delete g_ws;
      g_ws=NULL;
   }
}

//----------------------- Build JSON payloads ------------------------
string ActionFromEntry(const int entry, const int deal_type)
{
   if(entry==DEAL_ENTRY_IN)
      return (deal_type==DEAL_TYPE_BUY ? "BUY OPEN" : "SELL OPEN");
   if(entry==DEAL_ENTRY_OUT)
      return (deal_type==DEAL_TYPE_BUY ? "BUY CLOSE" : "SELL CLOSE");
   if(entry==DEAL_ENTRY_INOUT)  return "CLOSE & REVERSE";
   if(entry==DEAL_ENTRY_OUT_BY) return "CLOSE BY OPPOSITE";
   return "UNKNOWN";
}

string BuildSymbolMetadataJson(const string symbol)
{
   if(symbol == NULL || symbol == "")
      return "";

   int digits = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
   double point = SymbolInfoDouble(symbol, SYMBOL_POINT);
   double tick_size = SymbolInfoDouble(symbol, SYMBOL_TRADE_TICK_SIZE);
   double tick_value = SymbolInfoDouble(symbol, SYMBOL_TRADE_TICK_VALUE);
   double contract_size = SymbolInfoDouble(symbol, SYMBOL_TRADE_CONTRACT_SIZE);
   double volume_step = SymbolInfoDouble(symbol, SYMBOL_VOLUME_STEP);
   double volume_min = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MIN);
   double volume_max = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MAX);
   string currency_base = SymbolInfoString(symbol, SYMBOL_CURRENCY_BASE);
   string currency_profit = SymbolInfoString(symbol, SYMBOL_CURRENCY_PROFIT);
   string currency_margin = SymbolInfoString(symbol, SYMBOL_CURRENCY_MARGIN);

   return StringFormat(
      ",\"symbol_digits\":%d,\"symbol_point\":%.10f,\"symbol_tick_size\":%.10f,"
      "\"symbol_tick_value\":%.10f,\"symbol_contract_size\":%.10f,"
      "\"symbol_volume_step\":%.8f,\"symbol_volume_min\":%.8f,\"symbol_volume_max\":%.8f,"
      "\"symbol_currency_base\":\"%s\",\"symbol_currency_profit\":\"%s\",\"symbol_currency_margin\":\"%s\"",
      digits,
      point,
      tick_size,
      tick_value,
      contract_size,
      volume_step,
      volume_min,
      volume_max,
      JsonEscape(currency_base),
      JsonEscape(currency_profit),
      JsonEscape(currency_margin)
   );
}

string BuildAccountFundsJson()
{
   return StringFormat(
      ",\"account_balance\":%.8f,\"account_equity\":%.8f",
      AccountInfoDouble(ACCOUNT_BALANCE),
      AccountInfoDouble(ACCOUNT_EQUITY)
   );
}

string BuildHeldPositionsJson()
{
   string out = "";
   int appended = 0;
   for(int i=PositionsTotal() - 1; i>=0; i--)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0 || !PositionSelectByTicket(ticket))
         continue;

      string symbol = PositionGetString(POSITION_SYMBOL);
      long position_id = (long)PositionGetInteger(POSITION_IDENTIFIER);
      double volume = PositionGetDouble(POSITION_VOLUME);
      double price_open = PositionGetDouble(POSITION_PRICE_OPEN);
      double sl = PositionGetDouble(POSITION_SL);
      double tp = PositionGetDouble(POSITION_TP);
      string comment = PositionGetString(POSITION_COMMENT);

      string item = StringFormat(
         "{\"ticket\":%I64u,\"position\":%I64d,\"order\":0,\"symbol\":\"%s\",\"volume\":%.8f,\"price_open\":%.10f,\"sl\":%.10f,\"tp\":%.10f,\"comment\":\"%s\"}",
         ticket,
         position_id,
         JsonEscape(symbol),
         volume,
         price_open,
         sl,
         tp,
         JsonEscape(comment)
      );
      if(appended > 0)
         out += ",";
      out += item;
      appended++;
   }
   return out;
}

bool TryFindOpenPositionVolumeById(const long position_id, const string symbol, double &volume_after)
{
   for(int i=PositionsTotal() - 1; i>=0; i--)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0 || !PositionSelectByTicket(ticket))
         continue;
      if(position_id > 0 && (long)PositionGetInteger(POSITION_IDENTIFIER) != position_id)
         continue;
      if(symbol != "" && PositionGetString(POSITION_SYMBOL) != symbol)
         continue;

      volume_after = PositionGetDouble(POSITION_VOLUME);
      return true;
   }
   return false;
}

string BuildDealPositionSnapshotJson(const int entry, const long position_id, const string symbol, const double close_volume)
{
   if(entry != DEAL_ENTRY_OUT && entry != DEAL_ENTRY_OUT_BY)
      return "";

   double position_volume_after = 0.0;
   TryFindOpenPositionVolumeById(position_id, symbol, position_volume_after);
   double position_volume_before = close_volume + position_volume_after;
   return StringFormat(
      ",\"position_volume_before\":%.8f,\"position_volume_after\":%.8f",
      position_volume_before,
      position_volume_after
   );
}

string BuildDealJson(const ulong deal_ticket, const datetime enqueue_time)
{
   // 限定一个较小窗口，确保该成交被加载进本地历史
   datetime now = TimeCurrent();
   datetime from = enqueue_time - 60;
   if(from < 0) from = 0;
   HistorySelect(from, now);

   if(!HistoryDealSelect(deal_ticket))
      return "";

   datetime t = (datetime)HistoryDealGetInteger(deal_ticket, DEAL_TIME);
   if(t==0) return "";

   string symbol = HistoryDealGetString(deal_ticket, DEAL_SYMBOL);
   double volume = HistoryDealGetDouble(deal_ticket, DEAL_VOLUME);
   double price  = HistoryDealGetDouble(deal_ticket, DEAL_PRICE);
   int dtype     = (int)HistoryDealGetInteger(deal_ticket, DEAL_TYPE);
   int entry     = (int)HistoryDealGetInteger(deal_ticket, DEAL_ENTRY);

   long pos_id   = (long)HistoryDealGetInteger(deal_ticket, DEAL_POSITION_ID);
   long ord_id   = (long)HistoryDealGetInteger(deal_ticket, DEAL_ORDER);
   long magic    = (long)HistoryDealGetInteger(deal_ticket, DEAL_MAGIC);
   string cmt    = HistoryDealGetString(deal_ticket, DEAL_COMMENT);

   long login=(long)AccountInfoInteger(ACCOUNT_LOGIN);
   string srv=AccountInfoString(ACCOUNT_SERVER);

   string action = ActionFromEntry(entry, dtype);
   string event_id = StringFormat("%I64d-DEAL-%I64u", login, deal_ticket);
   string account_funds = BuildAccountFundsJson();
   string symbol_meta = BuildSymbolMetadataJson(symbol);
   string position_snapshot = BuildDealPositionSnapshotJson(entry, pos_id, symbol, volume);

   return StringFormat(
      "{\"type\":\"DEAL\",\"event_id\":\"%s\",\"login\":%I64d,\"server\":\"%s\","
      "\"deal\":%I64u,\"order\":%I64d,\"position\":%I64d,"
      "\"symbol\":\"%s\",\"action\":\"%s\",\"volume\":%.4f,\"price\":%.10f,"
      "\"deal_type\":%d,\"entry\":%d,\"magic\":%I64d,"
      "\"comment\":\"%s\",\"time\":\"%s\"%s%s%s}",
      JsonEscape(event_id),
      login, JsonEscape(srv),
      deal_ticket, ord_id, pos_id,
      JsonEscape(symbol), JsonEscape(action),
      volume, price,
      dtype, entry, magic,
      JsonEscape(cmt),
      TimeToString(t, TIME_DATE|TIME_SECONDS),
      account_funds,
      position_snapshot,
      symbol_meta
   );
}

string OrderEventName(const int trans_type)
{
   if(trans_type==TRADE_TRANSACTION_ORDER_ADD)    return "ORDER_ADD";
   if(trans_type==TRADE_TRANSACTION_ORDER_UPDATE) return "ORDER_UPDATE";
   if(trans_type==TRADE_TRANSACTION_ORDER_DELETE) return "ORDER_DELETE";
   if(trans_type==TRADE_TRANSACTION_POSITION)     return "ORDER_UPDATE";
   return "ORDER_EVENT";
}

long PriceFingerprint(const double value)
{
   return (long)MathRound(value * 100000.0);
}

long VolumeFingerprint(const double value)
{
   return (long)MathRound(value * 10000.0);
}

string BuildPositionUpdateJson(const ulong position_ticket, const datetime enqueue_time)
{
   if(position_ticket == 0 || !PositionSelectByTicket(position_ticket))
      return "";

   long login = (long)AccountInfoInteger(ACCOUNT_LOGIN);
   string srv = AccountInfoString(ACCOUNT_SERVER);
   string symbol = PositionGetString(POSITION_SYMBOL);
   long position_id = (long)PositionGetInteger(POSITION_IDENTIFIER);
   long position_type = (long)PositionGetInteger(POSITION_TYPE);
   long magic = (long)PositionGetInteger(POSITION_MAGIC);
   double volume = PositionGetDouble(POSITION_VOLUME);
   double price_open = PositionGetDouble(POSITION_PRICE_OPEN);
   double sl = PositionGetDouble(POSITION_SL);
   double tp = PositionGetDouble(POSITION_TP);
   string comment = PositionGetString(POSITION_COMMENT);
   datetime t_setup = (datetime)PositionGetInteger(POSITION_TIME);
   datetime t_done = (datetime)PositionGetInteger(POSITION_TIME_UPDATE);
   int order_type = position_type == POSITION_TYPE_SELL ? ORDER_TYPE_SELL : ORDER_TYPE_BUY;
   string account_funds = BuildAccountFundsJson();
   string symbol_meta = BuildSymbolMetadataJson(symbol);

   string event_id = StringFormat("%I64d-ORDER_UPDATE-POS-%I64d-%I64d-%I64d-%I64d-%I64d",
                                  login,
                                  position_id,
                                  (long)enqueue_time,
                                  PriceFingerprint(price_open),
                                  PriceFingerprint(sl),
                                  PriceFingerprint(tp));

   return StringFormat(
      "{\"type\":\"ORDER\",\"event\":\"ORDER_UPDATE\",\"scope\":\"ACTIVE\",\"event_id\":\"%s\","
      "\"login\":%I64d,\"server\":\"%s\","
      "\"order\":0,\"position\":%I64d,\"symbol\":\"%s\",\"order_type\":%d,\"order_state\":1,"
      "\"vol_init\":%.4f,\"vol_cur\":%.4f,\"price_open\":%.10f,\"sl\":%.10f,\"tp\":%.10f,"
      "\"magic\":%I64d,\"comment\":\"%s\","
      "\"time_setup\":\"%s\",\"time_done\":\"%s\"%s%s}",
      JsonEscape(event_id),
      login, JsonEscape(srv),
      position_id, JsonEscape(symbol), order_type, volume, volume, price_open, sl, tp,
      magic, JsonEscape(comment),
      TimeToString(t_setup, TIME_DATE|TIME_SECONDS),
      TimeToString(t_done, TIME_DATE|TIME_SECONDS),
      account_funds,
      symbol_meta
   );
}

string BuildOrderJson(const int trans_type, const ulong order_ticket, const ulong position_ticket, const datetime enqueue_time)
{
   string ev = OrderEventName(trans_type);

   if(trans_type == TRADE_TRANSACTION_POSITION)
      return BuildPositionUpdateJson(position_ticket, enqueue_time);

   // 优先 active；若已删除则尝试 history
   string scope="ACTIVE";
   bool ok = OrderSelect(order_ticket);
   if(!ok)
   {
      scope="HISTORY";
      datetime now = TimeCurrent();
      datetime from = enqueue_time - 120;
      if(from < 0) from = 0;
      HistorySelect(from, now);
      if(!HistoryOrderSelect(order_ticket))
         return "";
   }

   long login=(long)AccountInfoInteger(ACCOUNT_LOGIN);
   string srv=AccountInfoString(ACCOUNT_SERVER);

   string symbol;
   long   type, state, magic, position_id;
   double vol_init, vol_cur, price_open, sl, tp;
   datetime t_setup, t_done;
   string comment;

   if(scope=="ACTIVE")
   {
      symbol    = OrderGetString(ORDER_SYMBOL);
      type      = (long)OrderGetInteger(ORDER_TYPE);
      state     = (long)OrderGetInteger(ORDER_STATE);
      magic     = (long)OrderGetInteger(ORDER_MAGIC);
      vol_init  = OrderGetDouble(ORDER_VOLUME_INITIAL);
      vol_cur   = OrderGetDouble(ORDER_VOLUME_CURRENT);
      price_open= OrderGetDouble(ORDER_PRICE_OPEN);
      sl        = OrderGetDouble(ORDER_SL);
      tp        = OrderGetDouble(ORDER_TP);
      t_setup   = (datetime)OrderGetInteger(ORDER_TIME_SETUP);
      t_done    = (datetime)OrderGetInteger(ORDER_TIME_DONE);
      position_id = (long)OrderGetInteger(ORDER_POSITION_ID);
      comment   = OrderGetString(ORDER_COMMENT);
   }
   else
   {
      symbol    = HistoryOrderGetString(order_ticket, ORDER_SYMBOL);
      type      = (long)HistoryOrderGetInteger(order_ticket, ORDER_TYPE);
      state     = (long)HistoryOrderGetInteger(order_ticket, ORDER_STATE);
      magic     = (long)HistoryOrderGetInteger(order_ticket, ORDER_MAGIC);
      vol_init  = HistoryOrderGetDouble(order_ticket, ORDER_VOLUME_INITIAL);
      vol_cur   = HistoryOrderGetDouble(order_ticket, ORDER_VOLUME_CURRENT);
      price_open= HistoryOrderGetDouble(order_ticket, ORDER_PRICE_OPEN);
      sl        = HistoryOrderGetDouble(order_ticket, ORDER_SL);
      tp        = HistoryOrderGetDouble(order_ticket, ORDER_TP);
      t_setup   = (datetime)HistoryOrderGetInteger(order_ticket, ORDER_TIME_SETUP);
      t_done    = (datetime)HistoryOrderGetInteger(order_ticket, ORDER_TIME_DONE);
      position_id = (long)HistoryOrderGetInteger(order_ticket, ORDER_POSITION_ID);
      comment   = HistoryOrderGetString(order_ticket, ORDER_COMMENT);
   }

   if(position_id <= 0 && position_ticket > 0)
      position_id = (long)position_ticket;

   string event_id = trans_type == TRADE_TRANSACTION_ORDER_UPDATE
      ? StringFormat("%I64d-%s-%I64u-%I64d-%I64d-%I64d-%I64d-%I64d",
                     login, ev, order_ticket, (long)enqueue_time,
                     PriceFingerprint(price_open), PriceFingerprint(sl), PriceFingerprint(tp), VolumeFingerprint(vol_cur))
      : StringFormat("%I64d-%s-%I64u", login, ev, order_ticket);
   string account_funds = BuildAccountFundsJson();
   string symbol_meta = BuildSymbolMetadataJson(symbol);

   return StringFormat(
      "{\"type\":\"ORDER\",\"event\":\"%s\",\"scope\":\"%s\",\"event_id\":\"%s\","
      "\"login\":%I64d,\"server\":\"%s\","
      "\"order\":%I64u,\"position\":%I64d,\"symbol\":\"%s\",\"order_type\":%d,\"order_state\":%d,"
      "\"vol_init\":%.4f,\"vol_cur\":%.4f,\"price_open\":%.10f,\"sl\":%.10f,\"tp\":%.10f,"
      "\"magic\":%I64d,\"comment\":\"%s\","
      "\"time_setup\":\"%s\",\"time_done\":\"%s\"%s%s}",
      ev, scope, JsonEscape(event_id),
      login, JsonEscape(srv),
      order_ticket, position_id, JsonEscape(symbol), (int)type, (int)state,
      vol_init, vol_cur, price_open, sl, tp,
      magic, JsonEscape(comment),
      TimeToString(t_setup, TIME_DATE|TIME_SECONDS),
      TimeToString(t_done,  TIME_DATE|TIME_SECONDS),
      account_funds,
      symbol_meta
   );
}

//----------------------- Process queues -----------------------------
void ProcessPendingDeals()
{
   for(int i=0; i<ArraySize(g_pending_deals); )
   {
      PendingDeal pd = g_pending_deals[i];
      string json = BuildDealJson(pd.deal_ticket, pd.enqueue_time);

      if(json=="")
      {
         pd.tries++;
         g_pending_deals[i]=pd;

         if(pd.tries >= MaxDealRetries)
         {
            Print("Drop pending deal: ", pd.deal_ticket);
            PendingDealRemoveAt(i);
            continue;
         }
         i++;
         continue;
      }

      OutboxPush(json);
      PendingDealRemoveAt(i);
   }
}

void ProcessPendingOrders()
{
   for(int i=0; i<ArraySize(g_pending_orders); )
   {
      PendingOrder po = g_pending_orders[i];
      string json = BuildOrderJson(po.trans_type, po.order_ticket, po.position_ticket, po.enqueue_time);

      if(json=="")
      {
         po.tries++;
         g_pending_orders[i]=po;

         if(po.tries >= MaxOrderRetries)
         {
            PendingOrderRemoveAt(i);
            continue;
         }
         i++;
         continue;
      }

      OutboxPush(json);
      PendingOrderRemoveAt(i);
   }
}

void FlushOutbox()
{
   if(ArraySize(g_outbox)==0) return;
   if(!WS_Connect()) return;

   int sent=0;
   for(int i=0; i<ArraySize(g_outbox); )
   {
      if(!g_ws.send(g_outbox[i]))
      {
         // 发送失败：断开，保留 outbox，下轮重连再发
         g_ws.close();
         delete g_ws;
         g_ws=NULL;
         return;
      }

      OutboxRemoveAt(i);
      sent++;
      if(sent >= SendBatchPerTick) break;
   }
}

//----------------------- EA lifecycle -------------------------------
string ErrText(const int err)
{
   switch(err)
   {
      case 0:    return "OK";
      case 4014: return "ERR_FUNCTION_NOT_ALLOWED (function not allowed)";
      case 4016: return "ERR_INVALID_PARAMETER";
      case 4806: return "ERR_NO_CONNECTION";
      case 5004: return "ERR_SERVER_BUSY";
      case 5024: return "ERR_TOO_FREQUENT_REQUESTS";
      case 5272: return "ERR_NETSOCKET_CANNOT_CONNECT";
      case 5273: return "ERR_NETSOCKET_TIMEOUT";
      case 5274: return "ERR_NETSOCKET_INVALID_HANDLE";
      case 5275: return "ERR_NETSOCKET_NO_HOST";
      case 5276: return "ERR_NETSOCKET_NO_PORT";
      default:   return "ERR_" + IntegerToString(err);
   }
}
void DumpSocketDiagnostics(const string host, const uint port, const uint timeout_ms)
{
   PrintFormat("[ENV] PROGRAM_TYPE=%d TESTER=%d OPT=%d VISUAL=%d DEBUG=%d TRADE_ALLOWED=%d DATA_PATH=%s",
               (int)MQLInfoInteger(MQL_PROGRAM_TYPE),
               (int)MQLInfoInteger(MQL_TESTER),
               (int)MQLInfoInteger(MQL_OPTIMIZATION),
               (int)MQLInfoInteger(MQL_VISUAL_MODE),
               (int)MQLInfoInteger(MQL_DEBUG),
               (int)MQLInfoInteger(MQL_TRADE_ALLOWED),
               TerminalInfoString(TERMINAL_DATA_PATH));

   ResetLastError();
   int s = SocketCreate();
   int e1 = GetLastError();
   PrintFormat("[SOCKET] SocketCreate()=%d err=%d", s, e1);

   if(s != INVALID_HANDLE)
   {
      ResetLastError();
      bool ok = SocketConnect(s, host, port, timeout_ms);
      int e2 = GetLastError();
      PrintFormat("[SOCKET] SocketConnect(%s:%u) ok=%d err=%d", host, port, (int)ok, e2);
      SocketClose(s);
   }
}

int OnInit()
{
   //DumpNetEnv();

   ArrayResize(g_outbox, 0);
   ArrayResize(g_pending_deals, 0);
   ArrayResize(g_pending_orders, 0);

   EventSetMillisecondTimer(TimerPeriodMs);

   PrintFormat("MasterSignalSenderWS started. WsUrl=%s ACCOUNT_LOGIN=%I64d ACCOUNT_SERVER=%s",
               WsUrl,
               (long)AccountInfoInteger(ACCOUNT_LOGIN),
               AccountInfoString(ACCOUNT_SERVER));
   WS_Connect();
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   EventKillTimer();

   if(g_ws)
   {
      g_ws.close();
      delete g_ws;
      g_ws=NULL;
   }

   ArrayResize(g_outbox, 0);
   ArrayResize(g_pending_deals, 0);
   ArrayResize(g_pending_orders, 0);

   Print("MasterSignalSenderWS stopped.");
}

void OnTimer()
{
   WS_Pump();
   WS_SendHeartbeat();

   ProcessPendingDeals();
   ProcessPendingOrders();
   FlushOutbox();
}

// 捕捉交易事务：只入队，不在此处做网络 IO
void OnTradeTransaction(const MqlTradeTransaction& trans,
                        const MqlTradeRequest&    request,
                        const MqlTradeResult&     result)
{
   if(trans.type == TRADE_TRANSACTION_DEAL_ADD)
   {
      PendingDealPush((ulong)trans.deal);
      return;
   }

   if(trans.type == TRADE_TRANSACTION_ORDER_ADD ||
      trans.type == TRADE_TRANSACTION_ORDER_UPDATE ||
      trans.type == TRADE_TRANSACTION_ORDER_DELETE)
   {
      PendingOrderPush((int)trans.type, (ulong)trans.order, (ulong)trans.position);
      return;
   }

   if(trans.type == TRADE_TRANSACTION_POSITION)
   {
      PendingOrderPush((int)trans.type, (ulong)trans.order, (ulong)trans.position);
      return;
   }
}

