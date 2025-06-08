import { useParams } from 'react-router'
import { useEffect, useState } from 'react'
import {Table, Button, Modal, Popover} from 'antd'
import TaskHistorySteps from '../components/TaskHistorySteps'

export type StatusEntry = {
    instance: string,
    status: 'SCHEDULED' | 'ENQUEUED' | 'STARTED' | 'COMPLETED' | 'FAILED',
    scheduledAt?: string,
    enqueuedAt?: string,
    startedAt?: string,
    finishedAt?: string,
    info?: {
        errorMessage?: string,
        stackTrace?: string
    }
}

async function fetchTaskHistory(taskName: string): Promise<StatusEntry[]> {
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

function TaskHistoryTable() {
    const { taskName } = useParams()
    const [entries, setEntries] = useState<StatusEntry[]>([])

    useEffect(() => {
        if (taskName) {
            fetchTaskHistory(taskName).then(setEntries)
        }
    }, [taskName])

    const columns = [
        {
            title: 'Progress',
            key: 'progress',
            align: 'center',
            render: (_: any, entry: StatusEntry) => <TaskHistorySteps entry={entry} />
        },
        {
            title: 'Action',
            key: 'action',
            align: 'center',
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

    return (
        <>
            <Table dataSource={entries} columns={columns} rowKey="instance" pagination={false} />
        </>
    )
}

export default TaskHistoryTable