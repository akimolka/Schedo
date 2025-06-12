import { useParams } from 'react-router'
import { useEffect, useState } from 'react'
import {Table, Popover, Button, Space, message} from 'antd'
import TaskHistorySteps from '../components/TaskHistorySteps'

export type StatusEntry = {
    instance: string,
    status: 'SCHEDULED' | 'ENQUEUED' | 'STARTED' | 'COMPLETED' | 'FAILED' | 'CANCELLED',
    scheduledFor: string,
    createdAt: string,
    enqueuedAt?: string,
    startedAt?: string,
    finishedAt?: string,
    info?: {
        errorMessage?: string,
        stackTrace?: string
    }
}

type TaskStatus = 'RESUMED' | 'RUNNING' | 'FINISHED' | 'CANCELLED'

type DetailedTaskInfo = {
    history: StatusEntry[],
    executionStatus: TaskStatus | null,
    isCancelled: boolean | null
}

async function fetchTaskDetail(taskName: string): Promise<DetailedTaskInfo> {
    const response = await fetch(`http://localhost:8080/tasks/${taskName}`)
    return response.json()
}

function renderPopoverContent(info: { errorMessage?: string; stackTrace?: string }) {
    return (
        <div>
            {info.errorMessage && (
                <p><strong>Error:</strong> {info.errorMessage}</p>
            )}
            {info.stackTrace && (
                <div><strong>Stack trace:</strong><pre>{info.stackTrace}</pre></div>
            )}
            {!info.errorMessage && !info.stackTrace && (
                <p>No additional info</p>
            )}
        </div>
    )
}

const columns = [
    {
        title: 'Progress',
        key: 'progress',
        align: 'center' as const,
        render: (_: any, entry: StatusEntry) => <TaskHistorySteps entry={entry} />
    },
    {
        title: 'Action',
        key: 'action',
        align: 'center' as const,
        render: (_: any, entry: StatusEntry) =>
            entry.info ? (
                <Popover content={renderPopoverContent(entry.info)} trigger="click">
                    <a>{"Show additional information"}</a>
                </Popover>
            ) : (
                'No additional information'
            )
    }
]

function canCancel(detailedInfo: DetailedTaskInfo | null): boolean {
    if (!detailedInfo) return false
    const { executionStatus, isCancelled } = detailedInfo
    return (executionStatus === 'RUNNING' || executionStatus === 'RESUMED') && isCancelled === false
}

function canResume(detailedInfo: DetailedTaskInfo | null): boolean {
    if (!detailedInfo) return false
    const { executionStatus } = detailedInfo
    return executionStatus === 'FINISHED' || executionStatus === 'CANCELLED'
}

function TaskHistoryTable() {
    const { taskName } = useParams()
    const [detailedInfo, setDetailedInfo] = useState<DetailedTaskInfo | null>(null)
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (taskName) {
            fetchTaskDetail(taskName).then(setDetailedInfo)
        }
    }, [taskName])

    async function fetchDetails() {
        setLoading(true)
        try {
            const data = await fetchTaskDetail(taskName!)
            setDetailedInfo(data)
        } catch (error) {
            message.error('Failed to fetch task details')
        } finally {
            setLoading(false)
        }
    }

    const handleCancel = async () => {
        if (!taskName) return

        try {
            const response = await fetch(`http://localhost:8080/tasks/${taskName}/cancel`, {
                method: 'POST'
            })

            if (response.ok) {
                message.success('Task cancelled successfully')
                fetchDetails()
            } else if (response.status !== 409) {  // Ignore Conflict
                message.error('Failed to cancel task')
            }
        } catch (error) {
            message.error('Error cancelling task')
        }
    }

    const handleResume = async () => {
        if (!taskName) return

        try {
            const response = await fetch(`http://localhost:8080/tasks/${taskName}/resume`, {
                method: 'POST'
            })

            if (response.ok) {
                message.success('Task rescheduled successfully')
                fetchDetails()
            } else if (response.status !== 409) {  // Ignore Conflict
                message.error('Failed to reschedule task')
            }
        } catch (error) {
            message.error('Error rescheduling task')
        }
    }

    const historyEntries: StatusEntry[] = detailedInfo?.history ?? []

    return (
        <>
            <Space style={{ width: '100%', justifyContent: 'flex-end', marginBottom: 16 }}>
                <Button
                    type="default"
                    danger
                    disabled={!canCancel(detailedInfo) || loading}
                    onClick={handleCancel}
                >
                    Cancel
                </Button>
                <Button
                    type="default"
                    disabled={!canResume(detailedInfo) || loading}
                    onClick={handleResume}
                >
                    Reschedule
                </Button>
            </Space>
            <Table
                dataSource={historyEntries}
                columns={columns}
                rowKey="progress"
                loading={loading}
            />
        </>
    )
}

export default TaskHistoryTable
