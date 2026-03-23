import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { bindAccount, deleteAccount, getMyAccounts } from '../lib/api'
import { formatDateTime } from '../lib/format'
import { queryClient } from '../lib/query'
import { DataTable, EmptyState, ErrorState, LoadingState, PageHeader, StatusPill, Surface } from '../components/common'

async function invalidateAccountCollections() {
  await queryClient.invalidateQueries({ queryKey: ['accounts', 'mine'] })
  await queryClient.invalidateQueries({ queryKey: ['relations', 'mine'] })
  await queryClient.invalidateQueries({ queryKey: ['share', 'profile'] })
  await queryClient.invalidateQueries({ queryKey: ['monitor', 'overview'] })
}

export function AccountsPage() {
  const accountsQuery = useQuery({
    queryKey: ['accounts', 'mine'],
    queryFn: getMyAccounts,
  })
  const bindAccountMutation = useMutation({
    mutationFn: bindAccount,
    onSuccess: async () => {
      await invalidateAccountCollections()
    },
  })
  const deleteAccountMutation = useMutation({
    mutationFn: deleteAccount,
    onSuccess: async () => {
      await invalidateAccountCollections()
    },
  })

  if (accountsQuery.isPending) {
    return <LoadingState />
  }

  if (accountsQuery.error) {
    const error = accountsQuery.error
    return <ErrorState message={error instanceof Error ? error.message : '请求失败'} />
  }

  const accounts = accountsQuery.data!

  return (
    <div className="page-stack">
      <PageHeader
        title="我的 MT5 账户"
        description="管理当前登录用户名下的 master / follower 账户。解绑关系和删除 follower 账户都属于显式操作，不会因为保存 PAUSED 自动删除。"
      />

      <div className="two-column">
        <Surface title="绑定新账户" description="绑定时只创建或更新平台账户记录，不会自动下发 EA 的 websocket 参数。">
          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault()
              const form = new FormData(event.currentTarget)
              bindAccountMutation.mutate({
                brokerName: String(form.get('brokerName') || '').trim(),
                serverName: String(form.get('serverName') || '').trim(),
                mt5Login: Number(form.get('mt5Login')),
                credential: String(form.get('credential') || '').trim() || undefined,
                accountRole: String(form.get('accountRole') || 'FOLLOWER'),
                status: String(form.get('status') || 'ACTIVE'),
              })
            }}
          >
            <label className="field">
              <span>brokerName</span>
              <input name="brokerName" placeholder="EBC" required />
            </label>
            <label className="field">
              <span>serverName</span>
              <input name="serverName" placeholder="EBCFinancialGroupKY-Demo" required />
            </label>
            <label className="field">
              <span>mt5Login</span>
              <input min={1} name="mt5Login" placeholder="51631" required type="number" />
            </label>
            <label className="field">
              <span>accountRole</span>
              <select defaultValue="FOLLOWER" name="accountRole">
                <option value="MASTER">MASTER</option>
                <option value="FOLLOWER">FOLLOWER</option>
                <option value="BOTH">BOTH</option>
              </select>
            </label>
            <label className="field">
              <span>status</span>
              <select defaultValue="ACTIVE" name="status">
                <option value="ACTIVE">ACTIVE</option>
                <option value="PAUSED">PAUSED</option>
              </select>
            </label>
            <label className="field">
              <span>credential</span>
              <input name="credential" placeholder="留空表示仅走 WebSocket" />
            </label>
            {bindAccountMutation.error ? <div className="inline-error">{bindAccountMutation.error.message}</div> : null}
            {bindAccountMutation.data ? (
              <div className="inline-success">
                已绑定账户 #{bindAccountMutation.data.id} / {bindAccountMutation.data.serverName}:{bindAccountMutation.data.mt5Login}
              </div>
            ) : null}
            <button className="button button--primary" disabled={bindAccountMutation.isPending} type="submit">
              {bindAccountMutation.isPending ? '绑定中...' : '绑定 MT5 账户'}
            </button>
          </form>
        </Surface>

        <Surface title="使用说明" description="删除账户目前只开放给 follower，目的是避免误删主链路配置。">
          <div className="bullet-list">
            <div>把关系改成 PAUSED 再保存，只是暂停跟单，不是解绑。</div>
            <div>点击关系里的“解绑”才会删除 copy relation。</div>
            <div>点击账户里的“删除”只对 FOLLOWER 生效，会同时清理关系、风控和品种映射。</div>
            <div>EA 里的 WsUrl、BearerToken、FollowerAccountId 仍需要手工填写。</div>
          </div>
        </Surface>
      </div>

      <Surface title="账户列表" description="详情页可查看风控、关系、映射和分享配置。FOLLOWER 账户支持直接删除。">
        {accounts.length === 0 ? (
          <EmptyState title="还没有绑定 MT5 账户" message="先创建一组 master / follower 账户，再继续配置关系和 EA 参数。" />
        ) : (
          <DataTable
            headers={['ID', '角色', 'Broker / Server', 'Login', '状态', 'Credential', '更新时间', '操作']}
            rows={accounts.map((account) => [
              account.id,
              account.accountRole,
              `${account.brokerName} / ${account.serverName}`,
              account.mt5Login,
              <StatusPill value={account.status} />,
              account.credentialConfigured ? '已配置' : 'WebSocket-only',
              formatDateTime(account.updatedAt),
              <>
                <Link className="text-link" params={{ accountId: String(account.id) }} to="/app/accounts/$accountId">
                  查看
                </Link>
                {' '}
                {account.accountRole === 'FOLLOWER' ? (
                  <button
                    className="button button--ghost"
                    disabled={deleteAccountMutation.isPending}
                    onClick={() => {
                      if (!window.confirm(`删除 follower 账户 #${account.id} 后，会同时清理关系、风控和品种映射。确认继续？`)) {
                        return
                      }
                      deleteAccountMutation.mutate(account.id)
                    }}
                    type="button"
                  >
                    {deleteAccountMutation.isPending ? '删除中...' : '删除'}
                  </button>
                ) : null}
              </>,
            ])}
          />
        )}
        {deleteAccountMutation.error ? <div className="inline-error">{deleteAccountMutation.error.message}</div> : null}
      </Surface>
    </div>
  )
}
