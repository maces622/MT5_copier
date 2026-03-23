import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { getMonitorAccountDetail, getMonitorCommands, getMonitorDispatches } from '../lib/api'
import { formatDateTime } from '../lib/format'
import { DataTable, EmptyState, ErrorState, KeyValueGrid, LoadingState, PageHeader, StatusPill, Surface } from '../components/common'

const MONITOR_REFRESH_MS = 3000
const EMPTY_VALUE = '--'

export function MonitorAccountDetailPage() {
  const { accountId } = useParams({ from: '/app/monitor/accounts/$accountId' })
  const [eventKeyword, setEventKeyword] = useState('')

  const detailQuery = useQuery({
    queryKey: ['monitor', 'detail', accountId],
    queryFn: () => getMonitorAccountDetail(accountId),
    refetchInterval: MONITOR_REFRESH_MS,
    refetchIntervalInBackground: true,
  })
  const commandsQuery = useQuery({
    queryKey: ['monitor', 'commands', accountId],
    queryFn: () => getMonitorCommands(accountId),
    refetchInterval: MONITOR_REFRESH_MS,
    refetchIntervalInBackground: true,
  })
  const dispatchesQuery = useQuery({
    queryKey: ['monitor', 'dispatches', accountId],
    queryFn: () => getMonitorDispatches(accountId),
    refetchInterval: MONITOR_REFRESH_MS,
    refetchIntervalInBackground: true,
  })

  if (detailQuery.isPending || commandsQuery.isPending || dispatchesQuery.isPending) {
    return <LoadingState />
  }

  if (detailQuery.error || commandsQuery.error || dispatchesQuery.error) {
    const error = detailQuery.error || commandsQuery.error || dispatchesQuery.error
    return <ErrorState message={error instanceof Error ? error.message : 'Request failed'} />
  }

  const detail = detailQuery.data!
  const normalizedKeyword = eventKeyword.trim().toLowerCase()
  const commands = commandsQuery.data!.filter((command) =>
    !normalizedKeyword || command.masterEventId.toLowerCase().includes(normalizedKeyword),
  )
  const dispatches = dispatchesQuery.data!.filter((dispatch) =>
    !normalizedKeyword || dispatch.masterEventId.toLowerCase().includes(normalizedKeyword),
  )

  return (
    <div className="page-stack">
      <PageHeader
        title={`Monitor Detail #${accountId}`}
        description="Runtime state, websocket sessions, execution commands, and follower dispatches for this account."
      />

      <div className="two-column">
        <Surface title="Overview">
          {detail.overview ? (
            <KeyValueGrid
              items={[
                { label: 'accountKey', value: detail.overview.accountKey },
                { label: 'role', value: detail.overview.accountRole },
                { label: 'connection', value: <StatusPill value={detail.overview.connectionStatus} /> },
                { label: 'lastHeartbeat', value: formatDateTime(detail.overview.lastHeartbeatAt) },
                { label: 'pendingDispatch', value: detail.overview.pendingDispatchCount },
                { label: 'failedDispatch', value: detail.overview.failedDispatchCount },
              ]}
            />
          ) : (
            <EmptyState
              title="No overview"
              message="This account does not have aggregated monitor data yet."
            />
          )}
        </Surface>

        <Surface title="Runtime State">
          {detail.runtimeState ? (
            <KeyValueGrid
              items={[
                { label: 'connection', value: <StatusPill value={detail.runtimeState.connectionStatus} /> },
                { label: 'balance', value: detail.runtimeState.balance ?? 'n/a' },
                { label: 'equity', value: detail.runtimeState.equity ?? 'n/a' },
                { label: 'lastHello', value: formatDateTime(detail.runtimeState.lastHelloAt) },
                { label: 'lastHeartbeat', value: formatDateTime(detail.runtimeState.lastHeartbeatAt) },
                { label: 'lastSignalType', value: detail.runtimeState.lastSignalType ?? 'n/a' },
              ]}
            />
          ) : (
            <EmptyState
              title="No runtime state"
              message="This account has not reported a hello, heartbeat, or runtime snapshot yet."
            />
          )}
        </Surface>
      </div>

      <Surface
        title="Event Filter"
        description="Filter commands and dispatches for this account by masterEventId."
      >
        <div className="toolbar">
          <label className="field">
            <span>masterEventId</span>
            <input
              placeholder="51631-DEAL-31111080"
              value={eventKeyword}
              onChange={(event) => setEventKeyword(event.target.value)}
            />
          </label>
        </div>
      </Surface>

      <Surface title="WebSocket Sessions">
        {detail.wsSessions.length === 0 && detail.followerExecSessions.length === 0 ? (
          <EmptyState
            title="No sessions"
            message="This account currently has no active websocket session records."
          />
        ) : (
          <div className="two-column">
            <DataTable
              headers={['MT5 Session', 'Server', 'Login', 'Status', 'Last Heartbeat']}
              rows={detail.wsSessions.map((session) => [
                session.sessionId,
                session.server || EMPTY_VALUE,
                session.login ?? EMPTY_VALUE,
                <StatusPill value={session.connectionStatus} />,
                formatDateTime(session.lastHeartbeatAt),
              ])}
            />
            <DataTable
              headers={['Follower Session', 'Status', 'Server', 'Login', 'Last Heartbeat', 'Last Dispatch']}
              rows={detail.followerExecSessions.map((session) => [
                session.sessionId,
                <StatusPill value={session.connectionStatus} />,
                session.server || EMPTY_VALUE,
                session.login ?? EMPTY_VALUE,
                formatDateTime(session.lastHeartbeatAt),
                session.lastDispatchId
                  ? `${session.lastDispatchId} @ ${formatDateTime(session.lastDispatchSentAt)}`
                  : EMPTY_VALUE,
              ])}
            />
          </div>
        )}
      </Surface>

      <Surface title="Recent Commands">
        {commands.length === 0 ? (
          <EmptyState
            title="No commands"
            message="This account does not have execution command records yet."
          />
        ) : (
          <DataTable
            headers={['ID', 'Event', 'Type', 'Status', 'Volume', 'Reason', 'Created']}
            rows={commands.slice(0, 12).map((command) => [
              command.id,
              command.masterEventId,
              command.commandType,
              <StatusPill value={command.status} />,
              command.requestedVolume ?? EMPTY_VALUE,
              command.rejectReason || EMPTY_VALUE,
              formatDateTime(command.createdAt),
            ])}
          />
        )}
      </Surface>

      <Surface title="Recent Dispatches">
        {dispatches.length === 0 ? (
          <EmptyState
            title="No dispatches"
            message="This account does not have follower dispatch records yet."
          />
        ) : (
          <DataTable
            headers={['ID', 'Execution', 'Status', 'Message', 'Acked', 'Updated']}
            rows={dispatches.slice(0, 12).map((dispatch) => [
              dispatch.id,
              dispatch.executionCommandId,
              <StatusPill value={dispatch.status} />,
              dispatch.statusMessage || EMPTY_VALUE,
              formatDateTime(dispatch.ackedAt),
              formatDateTime(dispatch.updatedAt),
            ])}
          />
        )}
      </Surface>
    </div>
  )
}
