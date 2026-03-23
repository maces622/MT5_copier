//+------------------------------------------------------------------+
//| FollowerExecReceiverWS.mq5                                       |
//| Follower side: receive dispatch commands from Java via WS        |
//| Minimal skeleton: connect, hello, heartbeat, dispatch ack/fail   |
//+------------------------------------------------------------------+
#property strict
#property version "1.00"

#include "wsclient.mqh"

enum DispatchReplyMode
{
   REPLY_ACK = 0,
   REPLY_FAIL = 1,
   REPLY_LOG_ONLY = 2
};

enum DispatchExecutionMode
{
   EXECUTION_DRY_RUN = 0,
   EXECUTION_REAL = 1
};

input string WsUrl               = "ws://127.0.0.1:8080/ws/follower-exec?access_token=dev-follower-token";
input bool   UseCompression      = false;

input string SubProtocol         = "";
input string BearerToken         = "";

input long   FollowerAccountId   = 0;
input DispatchExecutionMode ExecutionMode = EXECUTION_DRY_RUN;
input DispatchReplyMode ReplyMode = REPLY_ACK;
input string AckMessage          = "dry-run accepted";
input string FailMessage         = "dry-run rejected";
input long   FollowerMagicNumber = 880001;
input string CommentPrefix       = "cp1";
input int    DefaultDeviationPoints = 20;

input int    TimeoutMs           = 5000;
input int    TimerPeriodMs       = 200;
input int    ReconnectIntervalMs = 1500;
input int    HeartbeatIntervalMs = 3000;
input int    MaxOutbox           = 1000;
input int    SendBatchPerTick    = 50;

struct PositionMapping
{
   long master_position_id;
   long master_order_id;
   ulong follower_position_ticket;
   string symbol;
};

struct PendingOrderMapping
{
   long master_order_id;
   ulong follower_order_ticket;
   string symbol;
   int master_order_type;
};

struct DispatchCommandData
{
   long dispatch_id;
   long execution_command_id;
   long master_deal_id;
   long master_order_id;
   long master_position_id;
   int master_order_type;
   int master_order_state;
   int max_slippage_points;
   bool slippage_enabled;
   string master_event_id;
   string command_type;
   string symbol;
   string slippage_mode;
   string instrument_category;
   string follower_action;
   string source_symbol;
   string symbol_currency_base;
   string symbol_currency_profit;
   string symbol_currency_margin;
   int symbol_digits;
   double max_slippage_pips;
   double max_slippage_price;
   double symbol_point;
   double symbol_tick_size;
   double symbol_tick_value;
   double symbol_contract_size;
   double symbol_volume_step;
   double symbol_volume_min;
   double symbol_volume_max;
   double volume;
   double close_ratio;
   double requested_price;
   double requested_sl;
   double requested_tp;
   bool close_all;
};

PositionMapping g_position_mappings[];
PendingOrderMapping g_pending_order_mappings[];

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

bool JsonTryGetRawValue(const string json, const string key, string &value)
{
   string pattern = "\"" + key + "\"";
   int key_pos = StringFind(json, pattern);
   if(key_pos < 0)
      return false;

   int colon_pos = StringFind(json, ":", key_pos + StringLen(pattern));
   if(colon_pos < 0)
      return false;

   int value_pos = colon_pos + 1;
   while(value_pos < StringLen(json))
   {
      ushort c = StringGetCharacter(json, value_pos);
      if(c!=' ' && c!='\t' && c!='\r' && c!='\n')
         break;
      value_pos++;
   }
   if(value_pos >= StringLen(json))
      return false;

   ushort first = StringGetCharacter(json, value_pos);
   if(first == '"')
   {
      string out = "";
      for(int i=value_pos + 1; i<StringLen(json); i++)
      {
         ushort c = StringGetCharacter(json, i);
         if(c == '\\')
         {
            if(i + 1 >= StringLen(json))
               break;
            ushort next = StringGetCharacter(json, i + 1);
            if(next == '"' || next == '\\' || next == '/')
               out += CharToString((uchar)next);
            else if(next == 'n')
               out += "\n";
            else if(next == 'r')
               out += "\r";
            else if(next == 't')
               out += "\t";
            else
               out += CharToString((uchar)next);
            i++;
            continue;
         }
         if(c == '"')
         {
            value = out;
            return true;
         }
         out += CharToString((uchar)c);
      }
      return false;
   }

   int end = value_pos;
   while(end < StringLen(json))
   {
      ushort c = StringGetCharacter(json, end);
      if(c == ',' || c == '}' || c == '\r' || c == '\n')
         break;
      end++;
   }

   value = StringSubstr(json, value_pos, end - value_pos);
   StringTrimLeft(value);
   StringTrimRight(value);
   return value != "";
}

bool JsonTryGetString(const string json, const string key, string &value)
{
   return JsonTryGetRawValue(json, key, value);
}

bool JsonTryGetLong(const string json, const string key, long &value)
{
   string raw;
   if(!JsonTryGetRawValue(json, key, raw))
      return false;

   StringTrimLeft(raw);
   StringTrimRight(raw);
   if(raw == "" || raw == "null")
      return false;

   value = (long)StringToInteger(raw);
   return true;
}

bool JsonTryGetDouble(const string json, const string key, double &value)
{
   string raw;
   if(!JsonTryGetRawValue(json, key, raw))
      return false;

   StringTrimLeft(raw);
   StringTrimRight(raw);
   if(raw == "" || raw == "null")
      return false;

   value = StringToDouble(raw);
   return true;
}

bool JsonTryGetInt(const string json, const string key, int &value)
{
   long raw = 0;
   if(!JsonTryGetLong(json, key, raw))
      return false;

   value = (int)raw;
   return true;
}

bool JsonTryGetBool(const string json, const string key, bool &value)
{
   string raw;
   if(!JsonTryGetRawValue(json, key, raw))
      return false;

   StringTrimLeft(raw);
   StringTrimRight(raw);
   string normalized = raw;
   StringToUpper(normalized);
   if(normalized == "TRUE" || normalized == "1")
   {
      value = true;
      return true;
   }
   if(normalized == "FALSE" || normalized == "0")
   {
      value = false;
      return true;
   }
   return false;
}

bool JsonTryGetObject(const string json, const string key, string &value)
{
   string pattern = "\"" + key + "\"";
   int key_pos = StringFind(json, pattern);
   if(key_pos < 0)
      return false;

   int colon_pos = StringFind(json, ":", key_pos + StringLen(pattern));
   if(colon_pos < 0)
      return false;

   int value_pos = colon_pos + 1;
   while(value_pos < StringLen(json))
   {
      ushort c = StringGetCharacter(json, value_pos);
      if(c!=' ' && c!='\t' && c!='\r' && c!='\n')
         break;
      value_pos++;
   }
   if(value_pos >= StringLen(json) || StringGetCharacter(json, value_pos) != '{')
      return false;

   int depth = 0;
   bool in_string = false;
   bool escaped = false;
   for(int i=value_pos; i<StringLen(json); i++)
   {
      ushort c = StringGetCharacter(json, i);
      if(in_string)
      {
         if(escaped)
         {
            escaped = false;
            continue;
         }
         if(c == '\\')
         {
            escaped = true;
            continue;
         }
         if(c == '"')
            in_string = false;
         continue;
      }

      if(c == '"')
      {
         in_string = true;
         continue;
      }
      if(c == '{')
      {
         depth++;
         continue;
      }
      if(c == '}')
      {
         depth--;
         if(depth == 0)
         {
            value = StringSubstr(json, value_pos, i - value_pos + 1);
            return true;
         }
      }
   }

   return false;
}

string ShortCommentPrefix()
{
   return "cp1";
}

string BuildTrackingComment(const long master_position_id, const long master_order_id)
{
   string prefix = ShortCommentPrefix();
   if(master_position_id > 0 && master_order_id > 0)
      return StringFormat("%s|mp=%I64d|mo=%I64d", prefix, master_position_id, master_order_id);
   if(master_position_id > 0)
      return StringFormat("%s|mp=%I64d", prefix, master_position_id);
   if(master_order_id > 0)
      return StringFormat("%s|mo=%I64d", prefix, master_order_id);
   return prefix;
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
      long master_order_id = 0;
      TryExtractTaggedLong(comment, "mo=", master_order_id);

      string item = StringFormat(
         "{\"ticket\":%I64u,\"position\":%I64d,\"order\":%I64d,\"symbol\":\"%s\",\"volume\":%.8f,\"price_open\":%.10f,\"sl\":%.10f,\"tp\":%.10f,\"comment\":\"%s\"}",
         ticket,
         position_id,
         master_order_id,
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

bool TryExtractTaggedLong(const string text, const string tag, long &value)
{
   int pos = StringFind(text, tag);
   if(pos < 0)
      return false;

   int start = pos + StringLen(tag);
   int end = start;
   while(end < StringLen(text))
   {
      ushort c = StringGetCharacter(text, end);
      if(c >= '0' && c <= '9')
      {
         end++;
         continue;
      }
      break;
   }
   if(end <= start)
      return false;

   value = (long)StringToInteger(StringSubstr(text, start, end - start));
   return value > 0;
}

int VolumeDigitsFromStep(const double step)
{
   if(step <= 0.0)
      return 2;

   double probe = step;
   int digits = 0;
   while(digits < 8 && MathAbs(probe - MathRound(probe)) > 1e-8)
   {
      probe *= 10.0;
      digits++;
   }
   return digits;
}

double NormalizeVolumeForSymbol(const string symbol, const double raw_volume)
{
   double step = SymbolInfoDouble(symbol, SYMBOL_VOLUME_STEP);
   double min_volume = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MIN);
   double max_volume = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MAX);
   if(step <= 0.0)
      step = 0.01;
   if(min_volume <= 0.0)
      min_volume = step;
   if(max_volume <= 0.0)
      max_volume = raw_volume;

   double bounded = raw_volume;
   if(bounded > max_volume)
      bounded = max_volume;
   if(bounded < min_volume)
      bounded = min_volume;

   double steps = bounded / step;
   double normalized = MathRound(steps) * step;
   return NormalizeDouble(normalized, VolumeDigitsFromStep(step));
}

double NormalizeCloseVolumeForSymbol(
   const string symbol,
   const double current_volume,
   const double requested_volume,
   const bool close_all
)
{
   double step = SymbolInfoDouble(symbol, SYMBOL_VOLUME_STEP);
   double min_volume = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MIN);
   if(step <= 0.0)
      step = 0.01;
   if(min_volume <= 0.0)
      min_volume = step;

   if(close_all || requested_volume >= current_volume - (step / 2.0))
      return NormalizeDouble(current_volume, VolumeDigitsFromStep(step));

   if(requested_volume < min_volume)
      return 0.0;

   double normalized = MathFloor((requested_volume + 1e-10) / step) * step;
   normalized = NormalizeDouble(normalized, VolumeDigitsFromStep(step));
   if(normalized < min_volume)
      return 0.0;
   if(normalized >= current_volume)
   {
      double previous_step = MathFloor(((current_volume - step) + 1e-10) / step) * step;
      previous_step = NormalizeDouble(previous_step, VolumeDigitsFromStep(step));
      if(previous_step < min_volume)
         return 0.0;
      return previous_step;
   }
   return normalized;
}

double NormalizePriceForSymbol(const string symbol, const double raw_price)
{
   int digits = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
   return NormalizeDouble(raw_price, digits);
}

bool EnsureTradingReady(const string symbol, string &reason)
{
   if(!MQLInfoInteger(MQL_TRADE_ALLOWED))
   {
      reason = "MQL trade is disabled for this EA";
      return false;
   }
   if(!TerminalInfoInteger(TERMINAL_TRADE_ALLOWED))
   {
      reason = "Terminal trade is disabled";
      return false;
   }
   if(!AccountInfoInteger(ACCOUNT_TRADE_ALLOWED))
   {
      reason = "Account trade is disabled";
      return false;
   }
   if(symbol == "")
   {
      reason = "Symbol is empty";
      return false;
   }
   if(!SymbolSelect(symbol, true))
   {
      reason = "Failed to select symbol " + symbol;
      return false;
   }
   if((ENUM_SYMBOL_TRADE_MODE)SymbolInfoInteger(symbol, SYMBOL_TRADE_MODE) == SYMBOL_TRADE_MODE_DISABLED)
   {
      reason = "Symbol trade mode is disabled for " + symbol;
      return false;
   }
   return true;
}

ENUM_ORDER_TYPE_FILLING ResolveFillingType(const string symbol)
{
   long filling_mode = SymbolInfoInteger(symbol, SYMBOL_FILLING_MODE);
   if((filling_mode & SYMBOL_FILLING_FOK) == SYMBOL_FILLING_FOK)
      return ORDER_FILLING_FOK;
   if((filling_mode & SYMBOL_FILLING_IOC) == SYMBOL_FILLING_IOC)
      return ORDER_FILLING_IOC;
   return ORDER_FILLING_RETURN;
}

bool IsTradeRetcodeSuccess(const uint retcode)
{
   return retcode == TRADE_RETCODE_DONE
       || retcode == TRADE_RETCODE_DONE_PARTIAL
       || retcode == TRADE_RETCODE_PLACED
       || retcode == TRADE_RETCODE_NO_CHANGES;
}

string BuildTradeResultMessage(const bool sent, const int err, const MqlTradeResult &result)
{
   return StringFormat("sent=%d err=%d retcode=%u deal=%I64u order=%I64u comment=%s",
                       (int)sent, err, result.retcode, result.deal, result.order, result.comment);
}

void UpsertPositionMapping(
   const long master_position_id,
   const long master_order_id,
   const ulong follower_position_ticket,
   const string symbol
)
{
   for(int i=0; i<ArraySize(g_position_mappings); i++)
   {
      if((master_position_id > 0 && g_position_mappings[i].master_position_id == master_position_id)
         || (master_order_id > 0 && g_position_mappings[i].master_order_id == master_order_id)
         || g_position_mappings[i].follower_position_ticket == follower_position_ticket)
      {
         if(master_position_id > 0)
            g_position_mappings[i].master_position_id = master_position_id;
         if(master_order_id > 0)
            g_position_mappings[i].master_order_id = master_order_id;
         g_position_mappings[i].follower_position_ticket = follower_position_ticket;
         g_position_mappings[i].symbol = symbol;
         return;
      }
   }

   int n = ArraySize(g_position_mappings);
   ArrayResize(g_position_mappings, n + 1);
   g_position_mappings[n].master_position_id = master_position_id;
   g_position_mappings[n].master_order_id = master_order_id;
   g_position_mappings[n].follower_position_ticket = follower_position_ticket;
   g_position_mappings[n].symbol = symbol;
}

void RemovePositionMapping(const long master_position_id, const long master_order_id, const ulong follower_position_ticket)
{
   for(int i=0; i<ArraySize(g_position_mappings); i++)
   {
      if((master_position_id > 0 && g_position_mappings[i].master_position_id == master_position_id)
         || (master_order_id > 0 && g_position_mappings[i].master_order_id == master_order_id)
         || (follower_position_ticket > 0 && g_position_mappings[i].follower_position_ticket == follower_position_ticket))
      {
         int last = ArraySize(g_position_mappings) - 1;
         if(i != last)
            g_position_mappings[i] = g_position_mappings[last];
         ArrayResize(g_position_mappings, last);
         return;
      }
   }
}

bool FindPositionMapping(const long master_position_id, const long master_order_id, ulong &follower_position_ticket)
{
   for(int i=0; i<ArraySize(g_position_mappings); i++)
   {
      if((master_position_id > 0 && g_position_mappings[i].master_position_id == master_position_id)
         || (master_order_id > 0 && g_position_mappings[i].master_order_id == master_order_id))
      {
         follower_position_ticket = g_position_mappings[i].follower_position_ticket;
         return true;
      }
   }
   return false;
}

void UpsertPendingOrderMapping(
   const long master_order_id,
   const ulong follower_order_ticket,
   const string symbol,
   const int master_order_type
)
{
   for(int i=0; i<ArraySize(g_pending_order_mappings); i++)
   {
      if((master_order_id > 0 && g_pending_order_mappings[i].master_order_id == master_order_id)
         || g_pending_order_mappings[i].follower_order_ticket == follower_order_ticket)
      {
         g_pending_order_mappings[i].master_order_id = master_order_id;
         g_pending_order_mappings[i].follower_order_ticket = follower_order_ticket;
         g_pending_order_mappings[i].symbol = symbol;
         g_pending_order_mappings[i].master_order_type = master_order_type;
         return;
      }
   }

   int n = ArraySize(g_pending_order_mappings);
   ArrayResize(g_pending_order_mappings, n + 1);
   g_pending_order_mappings[n].master_order_id = master_order_id;
   g_pending_order_mappings[n].follower_order_ticket = follower_order_ticket;
   g_pending_order_mappings[n].symbol = symbol;
   g_pending_order_mappings[n].master_order_type = master_order_type;
}

void RemovePendingOrderMapping(const long master_order_id, const ulong follower_order_ticket)
{
   for(int i=0; i<ArraySize(g_pending_order_mappings); i++)
   {
      if((master_order_id > 0 && g_pending_order_mappings[i].master_order_id == master_order_id)
         || (follower_order_ticket > 0 && g_pending_order_mappings[i].follower_order_ticket == follower_order_ticket))
      {
         int last = ArraySize(g_pending_order_mappings) - 1;
         if(i != last)
            g_pending_order_mappings[i] = g_pending_order_mappings[last];
         ArrayResize(g_pending_order_mappings, last);
         return;
      }
   }
}

bool FindPendingOrderMapping(const long master_order_id, ulong &follower_order_ticket)
{
   for(int i=0; i<ArraySize(g_pending_order_mappings); i++)
   {
      if(master_order_id > 0 && g_pending_order_mappings[i].master_order_id == master_order_id)
      {
         follower_order_ticket = g_pending_order_mappings[i].follower_order_ticket;
         return true;
      }
   }
   return false;
}

bool ScanOpenPositionsForMapping(
   const long master_position_id,
   const long master_order_id,
   const string symbol,
   ulong &follower_position_ticket
)
{
   for(int i=PositionsTotal() - 1; i>=0; i--)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0 || !PositionSelectByTicket(ticket))
         continue;
      if(symbol != "" && PositionGetString(POSITION_SYMBOL) != symbol)
         continue;

      string comment = PositionGetString(POSITION_COMMENT);
      long comment_master_position_id = 0;
      long comment_master_order_id = 0;
      bool match_position = master_position_id > 0 && TryExtractTaggedLong(comment, "mp=", comment_master_position_id)
                            && comment_master_position_id == master_position_id;
      bool match_order = master_order_id > 0 && TryExtractTaggedLong(comment, "mo=", comment_master_order_id)
                         && comment_master_order_id == master_order_id;
      if(!match_position && !match_order)
         continue;

      follower_position_ticket = ticket;
      UpsertPositionMapping(comment_master_position_id, comment_master_order_id, ticket, PositionGetString(POSITION_SYMBOL));
      return true;
   }
   return false;
}

bool ScanOpenOrdersForMapping(const long master_order_id, const string symbol, ulong &follower_order_ticket)
{
   for(int i=OrdersTotal() - 1; i>=0; i--)
   {
      ulong ticket = OrderGetTicket(i);
      if(ticket == 0 || !OrderSelect(ticket))
         continue;
      if(symbol != "" && OrderGetString(ORDER_SYMBOL) != symbol)
         continue;

      string comment = OrderGetString(ORDER_COMMENT);
      long comment_master_order_id = 0;
      if(!TryExtractTaggedLong(comment, "mo=", comment_master_order_id) || comment_master_order_id != master_order_id)
         continue;

      follower_order_ticket = ticket;
      UpsertPendingOrderMapping(comment_master_order_id, ticket, OrderGetString(ORDER_SYMBOL), (int)OrderGetInteger(ORDER_TYPE));
      return true;
   }
   return false;
}

bool TryResolvePositionTicket(
   const long master_position_id,
   const long master_order_id,
   const string symbol,
   ulong &follower_position_ticket
)
{
   follower_position_ticket = 0;
   if(FindPositionMapping(master_position_id, master_order_id, follower_position_ticket))
   {
      if(follower_position_ticket > 0 && PositionSelectByTicket(follower_position_ticket))
         return true;
      RemovePositionMapping(master_position_id, master_order_id, follower_position_ticket);
      follower_position_ticket = 0;
   }
   return ScanOpenPositionsForMapping(master_position_id, master_order_id, symbol, follower_position_ticket);
}

bool TryResolvePendingOrderTicket(const long master_order_id, const string symbol, ulong &follower_order_ticket)
{
   follower_order_ticket = 0;
   if(FindPendingOrderMapping(master_order_id, follower_order_ticket))
   {
      if(follower_order_ticket > 0 && OrderSelect(follower_order_ticket))
         return true;
      RemovePendingOrderMapping(master_order_id, follower_order_ticket);
      follower_order_ticket = 0;
   }
   return ScanOpenOrdersForMapping(master_order_id, symbol, follower_order_ticket);
}

void RestoreMappingsFromTerminalState()
{
   ArrayResize(g_position_mappings, 0);
   ArrayResize(g_pending_order_mappings, 0);

   for(int i=PositionsTotal() - 1; i>=0; i--)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0 || !PositionSelectByTicket(ticket))
         continue;

      string comment = PositionGetString(POSITION_COMMENT);
      long master_position_id = 0;
      long master_order_id = 0;
      if(!TryExtractTaggedLong(comment, "mp=", master_position_id)
         && !TryExtractTaggedLong(comment, "mo=", master_order_id))
         continue;

      UpsertPositionMapping(master_position_id, master_order_id, ticket, PositionGetString(POSITION_SYMBOL));
   }

   for(int i=OrdersTotal() - 1; i>=0; i--)
   {
      ulong ticket = OrderGetTicket(i);
      if(ticket == 0 || !OrderSelect(ticket))
         continue;

      string comment = OrderGetString(ORDER_COMMENT);
      long master_order_id = 0;
      if(!TryExtractTaggedLong(comment, "mo=", master_order_id))
         continue;

      UpsertPendingOrderMapping(master_order_id, ticket, OrderGetString(ORDER_SYMBOL), (int)OrderGetInteger(ORDER_TYPE));
   }

   PrintFormat("Follower restored mappings: positions=%d pendingOrders=%d",
               ArraySize(g_position_mappings), ArraySize(g_pending_order_mappings));
}

bool ResolvePendingOrderType(const int master_order_type, ENUM_ORDER_TYPE &order_type, string &reason)
{
   switch(master_order_type)
   {
      case ORDER_TYPE_BUY_LIMIT:
      case ORDER_TYPE_SELL_LIMIT:
      case ORDER_TYPE_BUY_STOP:
      case ORDER_TYPE_SELL_STOP:
         order_type = (ENUM_ORDER_TYPE)master_order_type;
         return true;
      case ORDER_TYPE_BUY_STOP_LIMIT:
      case ORDER_TYPE_SELL_STOP_LIMIT:
         reason = "Stop-limit pending orders are not supported yet";
         return false;
      default:
         reason = "Unsupported pending order type " + IntegerToString(master_order_type);
         return false;
   }
}

int ResolveDeviationPoints(const DispatchCommandData &command)
{
   if(command.max_slippage_points > 0)
      return command.max_slippage_points;
   return DefaultDeviationPoints;
}

double ResolvePipSize(const DispatchCommandData &command)
{
   double point = SymbolInfoDouble(command.symbol, SYMBOL_POINT);
   int digits = (int)SymbolInfoInteger(command.symbol, SYMBOL_DIGITS);
   if(point <= 0.0 && command.symbol_point > 0.0)
      point = command.symbol_point;
   if(digits <= 0 && command.symbol_digits > 0)
      digits = command.symbol_digits;
   if(point <= 0.0)
      return 0.0;
   if(digits == 3 || digits == 5)
      return point * 10.0;
   return point;
}

bool ValidateMarketSlippage(const DispatchCommandData &command, const double local_price, string &reason)
{
   if(!command.slippage_enabled)
      return true;
   if(command.requested_price <= 0.0 || command.slippage_mode == "")
      return true;

   double diff = MathAbs(local_price - command.requested_price);
   if(command.slippage_mode == "PIPS")
   {
      double pip_size = ResolvePipSize(command);
      if(pip_size <= 0.0 || command.max_slippage_pips <= 0.0)
      {
         reason = "Invalid pip slippage configuration";
         return false;
      }

      double diff_pips = diff / pip_size;
      if(diff_pips - command.max_slippage_pips > 1e-8)
      {
         reason = StringFormat(
            "Slippage %.2f pips exceeds limit %.2f pips, masterPrice=%.10f localPrice=%.10f",
            diff_pips,
            command.max_slippage_pips,
            command.requested_price,
            local_price
         );
         return false;
      }
      return true;
   }

   if(command.slippage_mode == "PRICE")
   {
      if(command.max_slippage_price <= 0.0)
      {
         reason = "Invalid price slippage configuration";
         return false;
      }

      if(diff - command.max_slippage_price > 1e-8)
      {
         reason = StringFormat(
            "Price slippage %.10f exceeds limit %.10f, masterPrice=%.10f localPrice=%.10f",
            diff,
            command.max_slippage_price,
            command.requested_price,
            local_price
         );
         return false;
      }
      return true;
   }

   return true;
}

bool IsBuyAction(const string follower_action)
{
   return StringFind(follower_action, "BUY") == 0;
}

bool ParseDispatchCommand(const string json, DispatchCommandData &command, string &reason)
{
   command.dispatch_id = 0;
   command.execution_command_id = 0;
   command.master_deal_id = 0;
   command.master_order_id = 0;
   command.master_position_id = 0;
   command.master_order_type = -1;
   command.master_order_state = -1;
   command.max_slippage_points = 0;
   command.slippage_enabled = false;
   command.master_event_id = "";
   command.command_type = "";
   command.symbol = "";
   command.slippage_mode = "";
   command.instrument_category = "";
   command.follower_action = "";
   command.source_symbol = "";
   command.symbol_currency_base = "";
   command.symbol_currency_profit = "";
   command.symbol_currency_margin = "";
   command.symbol_digits = 0;
   command.max_slippage_pips = 0.0;
   command.max_slippage_price = 0.0;
   command.symbol_point = 0.0;
   command.symbol_tick_size = 0.0;
   command.symbol_tick_value = 0.0;
   command.symbol_contract_size = 0.0;
   command.symbol_volume_step = 0.0;
   command.symbol_volume_min = 0.0;
   command.symbol_volume_max = 0.0;
   command.volume = 0.0;
   command.close_ratio = 0.0;
   command.requested_price = 0.0;
   command.requested_sl = 0.0;
   command.requested_tp = 0.0;
   command.close_all = false;

   if(!JsonTryGetLong(json, "dispatchId", command.dispatch_id))
   {
      reason = "dispatchId is missing";
      return false;
   }

   JsonTryGetLong(json, "executionCommandId", command.execution_command_id);
   JsonTryGetString(json, "masterEventId", command.master_event_id);

   string payload_json = "";
   if(!JsonTryGetObject(json, "payload", payload_json))
   {
      reason = "payload object is missing";
      return false;
   }

   JsonTryGetString(payload_json, "commandType", command.command_type);
   JsonTryGetString(payload_json, "symbol", command.symbol);
   JsonTryGetString(payload_json, "followerAction", command.follower_action);
   JsonTryGetLong(payload_json, "masterDealId", command.master_deal_id);
   JsonTryGetLong(payload_json, "masterOrderId", command.master_order_id);
   JsonTryGetLong(payload_json, "masterPositionId", command.master_position_id);
   JsonTryGetInt(payload_json, "masterOrderType", command.master_order_type);
   JsonTryGetInt(payload_json, "masterOrderState", command.master_order_state);
   JsonTryGetInt(payload_json, "maxSlippagePoints", command.max_slippage_points);
   JsonTryGetDouble(payload_json, "volume", command.volume);
    JsonTryGetDouble(payload_json, "closeRatio", command.close_ratio);
    JsonTryGetBool(payload_json, "closeAll", command.close_all);
   JsonTryGetDouble(payload_json, "requestedPrice", command.requested_price);
   JsonTryGetDouble(payload_json, "requestedSl", command.requested_sl);
   JsonTryGetDouble(payload_json, "requestedTp", command.requested_tp);

   string slippage_policy_json = "";
   if(JsonTryGetObject(payload_json, "slippagePolicy", slippage_policy_json))
   {
      JsonTryGetBool(slippage_policy_json, "enabled", command.slippage_enabled);
      JsonTryGetString(slippage_policy_json, "mode", command.slippage_mode);
      JsonTryGetString(slippage_policy_json, "instrumentCategory", command.instrument_category);
      JsonTryGetDouble(slippage_policy_json, "maxPips", command.max_slippage_pips);
      JsonTryGetDouble(slippage_policy_json, "maxPrice", command.max_slippage_price);
   }

   string instrument_meta_json = "";
   if(JsonTryGetObject(payload_json, "instrumentMeta", instrument_meta_json))
   {
      JsonTryGetString(instrument_meta_json, "sourceSymbol", command.source_symbol);
      JsonTryGetInt(instrument_meta_json, "digits", command.symbol_digits);
      JsonTryGetDouble(instrument_meta_json, "point", command.symbol_point);
      JsonTryGetDouble(instrument_meta_json, "tickSize", command.symbol_tick_size);
      JsonTryGetDouble(instrument_meta_json, "tickValue", command.symbol_tick_value);
      JsonTryGetDouble(instrument_meta_json, "contractSize", command.symbol_contract_size);
      JsonTryGetDouble(instrument_meta_json, "volumeStep", command.symbol_volume_step);
      JsonTryGetDouble(instrument_meta_json, "volumeMin", command.symbol_volume_min);
      JsonTryGetDouble(instrument_meta_json, "volumeMax", command.symbol_volume_max);
      JsonTryGetString(instrument_meta_json, "currencyBase", command.symbol_currency_base);
      JsonTryGetString(instrument_meta_json, "currencyProfit", command.symbol_currency_profit);
      JsonTryGetString(instrument_meta_json, "currencyMargin", command.symbol_currency_margin);
   }

   if(command.master_order_type < 0)
   {
      string master_signal_json = "";
      if(JsonTryGetObject(payload_json, "masterSignal", master_signal_json))
         JsonTryGetInt(master_signal_json, "orderType", command.master_order_type);
   }

   if(command.command_type == "")
   {
      reason = "commandType is missing";
      return false;
   }
   if(command.symbol == "")
   {
      reason = "symbol is missing";
      return false;
   }

   return true;
}

bool ExecuteCreatePendingOrderInternal(
   const DispatchCommandData &command,
   string &status_message,
   const bool skip_existing_check
);
bool ExecuteCancelPendingOrderInternal(
   const ulong follower_order_ticket,
   const long master_order_id,
   string &status_message
);

bool ExecuteOpenPosition(const DispatchCommandData &command, string &status_message)
{
   ulong existing_ticket = 0;
   if(TryResolvePositionTicket(command.master_position_id, command.master_order_id, command.symbol, existing_ticket))
   {
      status_message = StringFormat("Local position already exists, ticket=%I64u", existing_ticket);
      return true;
   }

   if(!EnsureTradingReady(command.symbol, status_message))
      return false;

   double volume = NormalizeVolumeForSymbol(command.symbol, command.volume);
   if(volume <= 0.0)
   {
      status_message = "Requested volume is invalid";
      return false;
   }

   MqlTick tick;
   if(!SymbolInfoTick(command.symbol, tick))
   {
      status_message = "Failed to read market tick for " + command.symbol;
      return false;
   }

   MqlTradeRequest request;
   MqlTradeResult result;
   ZeroMemory(request);
   ZeroMemory(result);

   request.action = TRADE_ACTION_DEAL;
   request.symbol = command.symbol;
   request.volume = volume;
   request.type = IsBuyAction(command.follower_action) ? ORDER_TYPE_BUY : ORDER_TYPE_SELL;
   request.price = request.type == ORDER_TYPE_BUY ? tick.ask : tick.bid;
   if(!ValidateMarketSlippage(command, request.price, status_message))
      return false;
   request.sl = command.requested_sl > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_sl) : 0.0;
   request.tp = command.requested_tp > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_tp) : 0.0;
   request.deviation = (ulong)MathMax(0, ResolveDeviationPoints(command));
   request.magic = (ulong)MathMax(0, FollowerMagicNumber);
   request.type_filling = ResolveFillingType(command.symbol);
   request.comment = BuildTrackingComment(command.master_position_id, command.master_order_id);

   ResetLastError();
   bool sent = OrderSend(request, result);
   int err = GetLastError();
   if(!sent || !IsTradeRetcodeSuccess(result.retcode))
   {
      status_message = "OPEN_POSITION failed: " + BuildTradeResultMessage(sent, err, result);
      return false;
   }

   ulong follower_position_ticket = 0;
   if(!TryResolvePositionTicket(command.master_position_id, command.master_order_id, command.symbol, follower_position_ticket))
      follower_position_ticket = result.order;

   if(follower_position_ticket > 0)
      UpsertPositionMapping(command.master_position_id, command.master_order_id, follower_position_ticket, command.symbol);

   status_message = StringFormat("OPEN_POSITION done, retcode=%u positionTicket=%I64u",
                                 result.retcode, follower_position_ticket);
   return true;
}

bool ExecuteClosePosition(const DispatchCommandData &command, string &status_message)
{
   ulong follower_position_ticket = 0;
   if(!TryResolvePositionTicket(command.master_position_id, command.master_order_id, command.symbol, follower_position_ticket))
   {
      status_message = "Local position is already absent";
      return true;
   }
   if(!PositionSelectByTicket(follower_position_ticket))
   {
      RemovePositionMapping(command.master_position_id, command.master_order_id, follower_position_ticket);
      status_message = "Local position is already absent";
      return true;
   }
   if(!EnsureTradingReady(command.symbol, status_message))
      return false;

   double current_volume = PositionGetDouble(POSITION_VOLUME);
   bool close_all = command.close_all;
   double target_volume = current_volume;
   if(!close_all)
   {
      if(command.close_ratio > 0.0)
         target_volume = current_volume * MathMin(1.0, command.close_ratio);
      else if(command.volume > 0.0)
         target_volume = MathMin(current_volume, command.volume);
   }

   double close_volume = NormalizeCloseVolumeForSymbol(command.symbol, current_volume, target_volume, close_all);
   if(close_volume <= 0.0)
   {
      status_message = "Close volume is below symbol min/step";
      return false;
   }

   long position_type = PositionGetInteger(POSITION_TYPE);
   MqlTick tick;
   if(!SymbolInfoTick(command.symbol, tick))
   {
      status_message = "Failed to read market tick for " + command.symbol;
      return false;
   }

   MqlTradeRequest request;
   MqlTradeResult result;
   ZeroMemory(request);
   ZeroMemory(result);

   request.action = TRADE_ACTION_DEAL;
   request.position = follower_position_ticket;
   request.symbol = command.symbol;
   request.volume = close_volume;
   request.type = position_type == POSITION_TYPE_BUY ? ORDER_TYPE_SELL : ORDER_TYPE_BUY;
   request.price = request.type == ORDER_TYPE_BUY ? tick.ask : tick.bid;
   request.deviation = (ulong)MathMax(0, ResolveDeviationPoints(command));
   request.magic = (ulong)MathMax(0, FollowerMagicNumber);
   request.type_filling = ResolveFillingType(command.symbol);
   request.comment = BuildTrackingComment(command.master_position_id, command.master_order_id);

   ResetLastError();
   bool sent = OrderSend(request, result);
   int err = GetLastError();
   if(!sent || !IsTradeRetcodeSuccess(result.retcode))
   {
      status_message = "CLOSE_POSITION failed: " + BuildTradeResultMessage(sent, err, result);
      return false;
   }

   if(!PositionSelectByTicket(follower_position_ticket))
      RemovePositionMapping(command.master_position_id, command.master_order_id, follower_position_ticket);

   status_message = StringFormat("CLOSE_POSITION done, retcode=%u positionTicket=%I64u",
                                 result.retcode, follower_position_ticket);
   return true;
}

bool ExecuteSyncTpSl(const DispatchCommandData &command, string &status_message)
{
   ulong follower_position_ticket = 0;
   if(!TryResolvePositionTicket(command.master_position_id, command.master_order_id, command.symbol, follower_position_ticket))
   {
      status_message = "Local position not found, skip TP/SL sync";
      return true;
   }
   if(!PositionSelectByTicket(follower_position_ticket))
   {
      RemovePositionMapping(command.master_position_id, command.master_order_id, follower_position_ticket);
      status_message = "Local position not found, skip TP/SL sync";
      return true;
   }
   if(!EnsureTradingReady(command.symbol, status_message))
      return false;

   MqlTradeRequest request;
   MqlTradeResult result;
   ZeroMemory(request);
   ZeroMemory(result);

   request.action = TRADE_ACTION_SLTP;
   request.position = follower_position_ticket;
   request.symbol = command.symbol;
   request.sl = command.requested_sl > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_sl) : 0.0;
   request.tp = command.requested_tp > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_tp) : 0.0;
   request.magic = (ulong)MathMax(0, FollowerMagicNumber);
   request.comment = BuildTrackingComment(command.master_position_id, command.master_order_id);

   ResetLastError();
   bool sent = OrderSend(request, result);
   int err = GetLastError();
   if(!sent || !IsTradeRetcodeSuccess(result.retcode))
   {
      status_message = "SYNC_TP_SL failed: " + BuildTradeResultMessage(sent, err, result);
      return false;
   }

   status_message = StringFormat("SYNC_TP_SL done, retcode=%u positionTicket=%I64u",
                                 result.retcode, follower_position_ticket);
   return true;
}

bool ExecuteCreatePendingOrderInternal(
   const DispatchCommandData &command,
   string &status_message,
   const bool skip_existing_check
)
{
   ulong existing_ticket = 0;
   if(!skip_existing_check && TryResolvePendingOrderTicket(command.master_order_id, command.symbol, existing_ticket))
   {
      status_message = StringFormat("Local pending order already exists, ticket=%I64u", existing_ticket);
      return true;
   }

   if(!EnsureTradingReady(command.symbol, status_message))
      return false;

   ENUM_ORDER_TYPE order_type;
   if(!ResolvePendingOrderType(command.master_order_type, order_type, status_message))
      return false;

   double volume = NormalizeVolumeForSymbol(command.symbol, command.volume);
   if(volume <= 0.0)
   {
      status_message = "Pending order volume is invalid";
      return false;
   }
   if(command.requested_price <= 0.0)
   {
      status_message = "Pending order price is missing";
      return false;
   }

   MqlTradeRequest request;
   MqlTradeResult result;
   ZeroMemory(request);
   ZeroMemory(result);

   request.action = TRADE_ACTION_PENDING;
   request.symbol = command.symbol;
   request.volume = volume;
   request.type = order_type;
   request.price = NormalizePriceForSymbol(command.symbol, command.requested_price);
   request.sl = command.requested_sl > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_sl) : 0.0;
   request.tp = command.requested_tp > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_tp) : 0.0;
   request.type_time = ORDER_TIME_GTC;
   request.type_filling = ResolveFillingType(command.symbol);
   request.magic = (ulong)MathMax(0, FollowerMagicNumber);
   request.comment = BuildTrackingComment(0, command.master_order_id);

   ResetLastError();
   bool sent = OrderSend(request, result);
   int err = GetLastError();
   if(!sent || !IsTradeRetcodeSuccess(result.retcode))
   {
      status_message = "CREATE_PENDING_ORDER failed: " + BuildTradeResultMessage(sent, err, result);
      return false;
   }

   ulong follower_order_ticket = 0;
   if(!TryResolvePendingOrderTicket(command.master_order_id, command.symbol, follower_order_ticket))
      follower_order_ticket = result.order;

   if(follower_order_ticket > 0)
      UpsertPendingOrderMapping(command.master_order_id, follower_order_ticket, command.symbol, command.master_order_type);

   status_message = StringFormat("CREATE_PENDING_ORDER done, retcode=%u orderTicket=%I64u",
                                 result.retcode, follower_order_ticket);
   return true;
}

bool ExecuteCreatePendingOrder(const DispatchCommandData &command, string &status_message)
{
   return ExecuteCreatePendingOrderInternal(command, status_message, false);
}

bool ExecuteCancelPendingOrderInternal(
   const ulong follower_order_ticket,
   const long master_order_id,
   string &status_message
)
{
   if(follower_order_ticket == 0 || !OrderSelect(follower_order_ticket))
   {
      RemovePendingOrderMapping(master_order_id, follower_order_ticket);
      status_message = "Local pending order is already absent";
      return true;
   }

   MqlTradeRequest request;
   MqlTradeResult result;
   ZeroMemory(request);
   ZeroMemory(result);

   request.action = TRADE_ACTION_REMOVE;
   request.order = follower_order_ticket;

   ResetLastError();
   bool sent = OrderSend(request, result);
   int err = GetLastError();
   if(!sent || !IsTradeRetcodeSuccess(result.retcode))
   {
      status_message = "CANCEL_PENDING_ORDER failed: " + BuildTradeResultMessage(sent, err, result);
      return false;
   }

   RemovePendingOrderMapping(master_order_id, follower_order_ticket);
   status_message = StringFormat("CANCEL_PENDING_ORDER done, retcode=%u orderTicket=%I64u",
                                 result.retcode, follower_order_ticket);
   return true;
}

bool ExecuteCancelPendingOrder(const DispatchCommandData &command, string &status_message)
{
   ulong follower_order_ticket = 0;
   if(!TryResolvePendingOrderTicket(command.master_order_id, command.symbol, follower_order_ticket))
   {
      status_message = "Local pending order is already absent";
      return true;
   }
   return ExecuteCancelPendingOrderInternal(follower_order_ticket, command.master_order_id, status_message);
}

bool ExecuteUpdatePendingOrder(const DispatchCommandData &command, string &status_message)
{
   ulong follower_order_ticket = 0;
   if(!TryResolvePendingOrderTicket(command.master_order_id, command.symbol, follower_order_ticket))
   {
      status_message = "Local pending order not found for update";
      return false;
   }
   if(!OrderSelect(follower_order_ticket))
   {
      RemovePendingOrderMapping(command.master_order_id, follower_order_ticket);
      status_message = "Local pending order not found for update";
      return false;
   }
   if(!EnsureTradingReady(command.symbol, status_message))
      return false;

   ENUM_ORDER_TYPE desired_type;
   if(!ResolvePendingOrderType(command.master_order_type, desired_type, status_message))
      return false;

   double desired_volume = NormalizeVolumeForSymbol(command.symbol, command.volume);
   double desired_price = command.requested_price > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_price) : 0.0;
   double desired_sl = command.requested_sl > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_sl) : 0.0;
   double desired_tp = command.requested_tp > 0.0 ? NormalizePriceForSymbol(command.symbol, command.requested_tp) : 0.0;
   if(desired_price <= 0.0)
   {
      status_message = "Updated pending order price is missing";
      return false;
   }

   int current_type = (int)OrderGetInteger(ORDER_TYPE);
   double current_volume = OrderGetDouble(ORDER_VOLUME_CURRENT);
   double current_price = OrderGetDouble(ORDER_PRICE_OPEN);
   double current_sl = OrderGetDouble(ORDER_SL);
   double current_tp = OrderGetDouble(ORDER_TP);
   double step = SymbolInfoDouble(command.symbol, SYMBOL_VOLUME_STEP);
   double point = SymbolInfoDouble(command.symbol, SYMBOL_POINT);
   if(step <= 0.0)
      step = 0.01;
   if(point <= 0.0)
      point = 0.00001;

   bool requires_recreate = current_type != (int)desired_type
                            || MathAbs(current_volume - desired_volume) > step / 2.0;
   if(requires_recreate)
   {
      if(!ExecuteCancelPendingOrderInternal(follower_order_ticket, command.master_order_id, status_message))
         return false;
      return ExecuteCreatePendingOrderInternal(command, status_message, true);
   }

   bool no_changes = MathAbs(current_price - desired_price) < point / 2.0
                     && MathAbs(current_sl - desired_sl) < point / 2.0
                     && MathAbs(current_tp - desired_tp) < point / 2.0;
   if(no_changes)
   {
      status_message = StringFormat("Pending order already matches local state, ticket=%I64u", follower_order_ticket);
      return true;
   }

   MqlTradeRequest request;
   MqlTradeResult result;
   ZeroMemory(request);
   ZeroMemory(result);

   request.action = TRADE_ACTION_MODIFY;
   request.order = follower_order_ticket;
   request.symbol = command.symbol;
   request.price = desired_price;
   request.sl = desired_sl;
   request.tp = desired_tp;

   ResetLastError();
   bool sent = OrderSend(request, result);
   int err = GetLastError();
   if(!sent || !IsTradeRetcodeSuccess(result.retcode))
   {
      status_message = "UPDATE_PENDING_ORDER failed: " + BuildTradeResultMessage(sent, err, result);
      return false;
   }

   UpsertPendingOrderMapping(command.master_order_id, follower_order_ticket, command.symbol, command.master_order_type);
   status_message = StringFormat("UPDATE_PENDING_ORDER done, retcode=%u orderTicket=%I64u",
                                 result.retcode, follower_order_ticket);
   return true;
}

bool ExecuteDispatch(const DispatchCommandData &command, string &status_message)
{
   if(command.command_type == "OPEN_POSITION")
      return ExecuteOpenPosition(command, status_message);
   if(command.command_type == "CLOSE_POSITION")
      return ExecuteClosePosition(command, status_message);
   if(command.command_type == "SYNC_TP_SL")
      return ExecuteSyncTpSl(command, status_message);
   if(command.command_type == "CREATE_PENDING_ORDER")
      return ExecuteCreatePendingOrder(command, status_message);
   if(command.command_type == "UPDATE_PENDING_ORDER")
      return ExecuteUpdatePendingOrder(command, status_message);
   if(command.command_type == "CANCEL_PENDING_ORDER")
      return ExecuteCancelPendingOrder(command, status_message);

   status_message = "Unsupported commandType " + command.command_type;
   return false;
}

class CFollowerExecWsClient : public WebSocketClient<WebSocketConnectionHybi>
{
private:
   string m_host_socket;
   uint   m_port_num;
   string m_host_header;

public:
   CFollowerExecWsClient(const string address, const bool useCompression)
      : WebSocketClient<WebSocketConnectionHybi>(address, useCompression)
   {
      string parts[];
      URL::parse(address, parts);

      scheme = parts[URL_SCHEME];
      if(scheme != "ws" && scheme != "wss") scheme = "ws";

      m_host_socket = parts[URL_HOST];
      string port_s = parts[URL_PORT];
      m_port_num = (uint)StringToInteger(port_s);
      if(m_port_num == 0)
         m_port_num = (scheme == "wss" ? 443 : 80);

      url = BuildRequestTarget(address, parts[URL_PATH]);
      host = m_host_socket;
      port = (string)m_port_num;

      bool defaultPort = (scheme=="ws" && m_port_num==80) || (scheme=="wss" && m_port_num==443);
      m_host_header = defaultPort ? m_host_socket : (m_host_socket + ":" + (string)m_port_num);
      origin = (scheme=="wss" ? "https://" : "http://") + m_host_header;
   }

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
};

CFollowerExecWsClient *g_ws = NULL;
ulong g_last_connect_try = 0;
ulong g_last_heartbeat   = 0;
bool  g_hello_sent       = false;

string g_outbox[];

void OutboxPush(const string json)
{
   if(json=="")
      return;

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
   if(n==0 || i<0 || i>=n)
      return;
   if(i!=n-1) g_outbox[i]=g_outbox[n-1];
   ArrayResize(g_outbox, n-1);
}

string BuildCustomHeaders()
{
   string hdr = "";
   if(SubProtocol!="")
      hdr += "Sec-WebSocket-Protocol: " + SubProtocol + "\r\n";
   if(BearerToken!="")
      hdr += "Authorization: Bearer " + BearerToken + "\r\n";
   return hdr;
}

string BuildHelloJson()
{
   double balance = AccountInfoDouble(ACCOUNT_BALANCE);
   double equity = AccountInfoDouble(ACCOUNT_EQUITY);
   string positions = BuildHeldPositionsJson();
   if(FollowerAccountId > 0)
   {
      return StringFormat(
         "{\"type\":\"HELLO\",\"followerAccountId\":%I64d,\"balance\":%.8f,\"equity\":%.8f,\"ts\":\"%s\",\"positions\":[%s]}",
         FollowerAccountId,
         balance,
         equity,
         TimeToString(TimeCurrent(), TIME_DATE|TIME_SECONDS),
         positions
      );
   }

   long login = (long)AccountInfoInteger(ACCOUNT_LOGIN);
   string server = AccountInfoString(ACCOUNT_SERVER);
   return StringFormat(
      "{\"type\":\"HELLO\",\"login\":%I64d,\"server\":\"%s\",\"balance\":%.8f,\"equity\":%.8f,\"ts\":\"%s\",\"positions\":[%s]}",
      login,
      JsonEscape(server),
      balance,
      equity,
      TimeToString(TimeCurrent(), TIME_DATE|TIME_SECONDS),
      positions
   );
}

string BuildHeartbeatJson()
{
   double balance = AccountInfoDouble(ACCOUNT_BALANCE);
   double equity = AccountInfoDouble(ACCOUNT_EQUITY);
   string positions = BuildHeldPositionsJson();
   return StringFormat(
      "{\"type\":\"HEARTBEAT\",\"balance\":%.8f,\"equity\":%.8f,\"ts\":\"%s\",\"positions\":[%s]}",
      balance,
      equity,
      TimeToString(TimeCurrent(), TIME_DATE|TIME_SECONDS),
      positions
   );
}

string BuildAckJson(const long dispatch_id, const bool success, const string message)
{
   return StringFormat(
      "{\"type\":\"%s\",\"dispatchId\":%I64d,\"statusMessage\":\"%s\"}",
      success ? "ACK" : "FAIL",
      dispatch_id,
      JsonEscape(message)
   );
}

string ErrText(const int err)
{
   switch(err)
   {
      case 0:    return "OK";
      case 4014: return "ERR_FUNCTION_NOT_ALLOWED";
      case 4016: return "ERR_INVALID_PARAMETER";
      case 4806: return "ERR_NO_CONNECTION";
      case 5004: return "ERR_SERVER_BUSY";
      case 5024: return "ERR_TOO_FREQUENT_REQUESTS";
      case 5270: return "ERR_NETSOCKET_IO_ERROR";
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

   g_ws = new CFollowerExecWsClient(WsUrl, UseCompression);
   g_ws.setTimeOut(TimeoutMs);

   string hdr = BuildCustomHeaders();
   if(!g_ws.open(hdr))
   {
      int err = GetLastError();
      PrintFormat("Follower WS open failed: %s err=%d %s", WsUrl, err, ErrText(err));
      DumpSocketDiagnostics("127.0.0.1", 8080, (uint)TimeoutMs);
      delete g_ws;
      g_ws = NULL;
      return false;
   }

   g_hello_sent = false;
   PrintFormat("Follower WS connected. target=%s bearer_len=%d", WsUrl, StringLen(BearerToken));
   return true;
}

void WS_SendHelloIfNeeded()
{
   if(g_hello_sent)
      return;
   if(!WS_Connect())
      return;

   string hello = BuildHelloJson();
   if(!g_ws.send(hello))
   {
      g_ws.close();
      delete g_ws;
      g_ws = NULL;
      g_hello_sent = false;
      return;
   }

   g_hello_sent = true;
   Print("Follower HELLO sent: ", hello);
}

void WS_SendHeartbeat()
{
   ulong now = GetTickCount();
   if(now - g_last_heartbeat < (ulong)HeartbeatIntervalMs)
      return;
   g_last_heartbeat = now;

   if(!WS_Connect())
      return;

   string hb = BuildHeartbeatJson();
   if(!g_ws.send(hb))
   {
      g_ws.close();
      delete g_ws;
      g_ws = NULL;
      g_hello_sent = false;
   }
}

void FlushOutbox()
{
   if(ArraySize(g_outbox)==0)
      return;
   if(!WS_Connect())
      return;

   int sent = 0;
   for(int i=0; i<ArraySize(g_outbox); )
   {
      if(!g_ws.send(g_outbox[i]))
      {
         g_ws.close();
         delete g_ws;
         g_ws = NULL;
         g_hello_sent = false;
         return;
      }

      OutboxRemoveAt(i);
      sent++;
      if(sent >= SendBatchPerTick)
         break;
   }
}

void HandleHelloAck(const string json)
{
   long follower_account_id = 0;
   long pending_dispatches = 0;
   JsonTryGetLong(json, "followerAccountId", follower_account_id);
   JsonTryGetLong(json, "pendingDispatchCount", pending_dispatches);

   PrintFormat("Follower HELLO_ACK followerAccountId=%I64d pendingDispatchCount=%I64d",
               follower_account_id, pending_dispatches);
}

void HandleStatusAck(const string json)
{
   long dispatch_id = 0;
   string status = "";
   string status_message = "";
   JsonTryGetLong(json, "dispatchId", dispatch_id);
   JsonTryGetString(json, "status", status);
   JsonTryGetString(json, "statusMessage", status_message);

   PrintFormat("Follower STATUS_ACK dispatchId=%I64d status=%s statusMessage=%s",
               dispatch_id, status, status_message);
}

void QueueDispatchReply(const long dispatch_id, const bool success, const string message)
{
   if(dispatch_id <= 0)
      return;

   string reply = BuildAckJson(dispatch_id, success, message);
   OutboxPush(reply);
   PrintFormat("Follower queued %s for dispatchId=%I64d message=%s",
               success ? "ACK" : "FAIL", dispatch_id, message);
}

void ReplyToDispatchDryRun(const long dispatch_id)
{
   if(dispatch_id <= 0 || ReplyMode == REPLY_LOG_ONLY)
      return;

   bool ack = (ReplyMode == REPLY_ACK);
   QueueDispatchReply(dispatch_id, ack, ack ? AckMessage : FailMessage);
}

void HandleDispatch(const string json)
{
   DispatchCommandData command;
   string reason = "";
   if(!ParseDispatchCommand(json, command, reason))
   {
      PrintFormat("Follower DISPATCH parse failed: %s raw=%s", reason, json);
      return;
   }

   PrintFormat("Follower DISPATCH dispatchId=%I64d executionCommandId=%I64d commandType=%s symbol=%s action=%s volume=%.4f masterEventId=%s",
               command.dispatch_id, command.execution_command_id, command.command_type,
               command.symbol, command.follower_action, command.volume, command.master_event_id);
   Print("Follower DISPATCH raw=", json);

   if(ExecutionMode == EXECUTION_DRY_RUN)
   {
      ReplyToDispatchDryRun(command.dispatch_id);
      return;
   }

   string status_message = "";
   bool success = ExecuteDispatch(command, status_message);
   if(status_message == "")
      status_message = success ? "real execution finished" : "real execution failed";
   QueueDispatchReply(command.dispatch_id, success, status_message);
}

void HandleInboundJson(const string json)
{
   string type = "";
   if(!JsonTryGetString(json, "type", type))
   {
      Print("Follower inbound message missing type: ", json);
      return;
   }

   if(type == "HELLO_ACK")
   {
      HandleHelloAck(json);
      return;
   }
   if(type == "STATUS_ACK")
   {
      HandleStatusAck(json);
      return;
   }
   if(type == "DISPATCH")
   {
      HandleDispatch(json);
      return;
   }

   Print("Follower inbound unhandled message: ", json);
}

void WS_ProcessInboundMessages()
{
   if(!g_ws || !g_ws.isConnected())
      return;

   g_ws.checkMessages(false);
   while(true)
   {
      IWebSocketMessage *msg = g_ws.readMessage(false);
      if(msg == NULL)
         break;

      string payload = msg.getString();
      delete msg;

      if(payload == NULL || payload == "")
         continue;

      HandleInboundJson(payload);
   }
}

int OnInit()
{
   ArrayResize(g_outbox, 0);
   ArrayResize(g_position_mappings, 0);
   ArrayResize(g_pending_order_mappings, 0);
   RestoreMappingsFromTerminalState();
   EventSetMillisecondTimer(TimerPeriodMs);
   PrintFormat("FollowerExecReceiverWS started. WsUrl=%s ExecutionMode=%s login=%I64d server=%s",
               WsUrl,
               ExecutionMode == EXECUTION_REAL ? "REAL" : "DRY_RUN",
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
      g_ws = NULL;
   }

   ArrayResize(g_outbox, 0);
   ArrayResize(g_position_mappings, 0);
   ArrayResize(g_pending_order_mappings, 0);
   Print("FollowerExecReceiverWS stopped.");
}

void OnTimer()
{
   WS_Connect();
   WS_SendHelloIfNeeded();
   WS_ProcessInboundMessages();
   WS_SendHeartbeat();
   FlushOutbox();
}
