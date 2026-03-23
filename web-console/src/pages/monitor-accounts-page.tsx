import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { getMonitorOverview, getMyAccounts } from '../lib/api'
import { formatDateTime } from '../lib/format'
import { DataTable, EmptyState, ErrorState, LoadingState, PageHeader, StatusPill, Surface } from '../components/common'

const MONITOR_REFRESH_MS = 3000

export function MonitorAccountsPage() {
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const accountsQuery = useQuery({
    queryKey: ['accounts', 'mine'],
    queryFn: getMyAccounts,
    refetchInterval: MONITOR_REFRESH_MS,
    refetchIntervalInBackground: true,
  })
  const overviewQuery = useQuery({
    queryKey: ['monitor', 'overview'],
    queryFn: getMonitorOverview,
    refetchInterval: MONITOR_REFRESH_MS,
    refetchIntervalInBackground: true,
  })

  if (accountsQuery.isPending || overviewQuery.isPending) {
    return <LoadingState />
  }

  if (accountsQuery.error || overviewQuery.error) {
    const error = accountsQuery.error || overviewQuery.error
    return <ErrorState message={error instanceof Error ? error.message : 'Request failed'} />
  }

  const accountIds = new Set(accountsQuery.data!.map((account) => account.id))
  const normalizedKeyword = keyword.trim().toLowerCase()
  const rows = overviewQuery.data!
    .filter((row) => accountIds.has(row.accountId))
    .filter((row) => statusFilter === 'ALL' || row.connectionStatus === statusFilter)
    .filter((row) => {
      if (!normalizedKeyword) {
        return true
      }
      return [
        row.accountKey,
        row.brokerName,
        row.serverName,
        String(row.mt5Login),
        row.accountRole,
      ]
        .join(' ')
        .toLowerCase()
        .includes(normalizedKeyword)
    })

  return (
    <div className="page-stack">
      <PageHeader
        title="Account Monitor"
        description="Watch connection status, runtime-state freshness, and dispatch pressure for your accounts."
      />

      <Surface
        title="My Monitor View"
        description="The page refreshes automatically every few seconds so websocket and dispatch status stay current."
      >
        <div className="toolbar">
          <label className="field">
            <span>Search</span>
            <input
              placeholder="accountKey / broker / server / login"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </label>
          <label className="field">
            <span>Connection</span>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">ALL</option>
              <option value="CONNECTED">CONNECTED</option>
              <option value="STALE">STALE</option>
              <option value="DISCONNECTED">DISCONNECTED</option>
              <option value="UNKNOWN">UNKNOWN</option>
            </select>
          </label>
        </div>

        {rows.length === 0 ? (
          <EmptyState
            title="No monitor data"
            message="Make sure the account is bound and has reported at least one hello or heartbeat."
          />
        ) : (
          <DataTable
            headers={['Account', 'Role', 'Connection', 'Last Heartbeat', 'Pending', 'Failed', 'Detail']}
            rows={rows.map((row) => [
              row.accountKey,
              row.accountRole,
              <StatusPill value={row.connectionStatus} />,
              formatDateTime(row.lastHeartbeatAt),
              row.pendingDispatchCount,
              row.failedDispatchCount,
              <Link className="text-link" params={{ accountId: String(row.accountId) }} to="/app/monitor/accounts/$accountId">
                Open
              </Link>,
            ])}
          />
        )}
      </Surface>
    </div>
  )
}
