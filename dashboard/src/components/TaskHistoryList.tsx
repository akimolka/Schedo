import { useParams } from 'react-router'
import { useEffect, useState } from 'react'
import { List } from 'antd'
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

function TaskHistoryList() {
    const { taskName } = useParams()
    const [entries, setEntries] = useState<StatusEntry[]>([])

    useEffect(() => {
        if (taskName) {
            fetchTaskHistory(taskName).then(setEntries)
        }
    }, [taskName])

    return (
        <List
                dataSource={entries}
                renderItem={(entry) => (
                    <List.Item>
                        <TaskHistorySteps entry={entry} />
                    </List.Item>
                )}
            />
    )
}

export default TaskHistoryList