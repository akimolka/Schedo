import { Table } from 'antd'
import { useEffect, useState } from 'react'

// matches FailedTaskInfo from SchedoServer
type FailedTask = {
    name: string
    lastFailure: string // ISO offset date-time
    recovered: boolean
    errorMessage: string
}

const columns = [
    {
        title: 'Task Name',
        dataIndex: 'name',
        key: 'name',
    },
    {
        title: 'Last Failure',
        dataIndex: 'lastFailure',
        key: 'lastFailure',
    },
    {
        title: 'Recovered',
        dataIndex: 'recovered',
        key: 'recovered',
        render: (value: boolean) => value ? 'Yes' : 'No',
    },
    {
        title: 'Error',
        dataIndex: 'errorMessage',
        key: 'errorMessage',
    }
]

async function fetchFailed(): Promise<FailedTask[]> {
    const response = await fetch('http://localhost:8080/tasks/failed')
    return response.json()
}

function FailedTaskList() {
    const [items, setItems] = useState<FailedTask[]>([])

    useEffect(() => {
        fetchFailed().then(data => setItems(data))
    }, [])

    return <Table dataSource={items} columns={columns} />
}

export default FailedTaskList
