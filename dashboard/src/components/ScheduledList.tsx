import { Table } from 'antd'
import { useEffect, useState } from 'react'
import {Link} from "react-router";

// matches ScheduledTaskInstance from SchedoServer
type ScheduledTask = {
    id: string
    name: string
    executionTime: string // ISO offset date-time
}

const columns = [
    {
        title: 'Instance ID',
        dataIndex: 'id',
        key: 'id',
    },
    {
        title: 'Task Name',
        dataIndex: 'name',
        key: 'name',
        render: (text: string) => <Link to={`/tasks/${text}`}>{text}</Link>
    },
    {
        title: 'Execution Time',
        dataIndex: 'executionTime',
        key: 'executionTime',
    },
]

async function fetchScheduled(): Promise<ScheduledTask[]> {
    const response = await fetch('http://localhost:8080/tasks/scheduled')
    return response.json()
}

function ScheduledTaskList() {
    const [items, setItems] = useState<ScheduledTask[]>([])

    useEffect(() => {
        fetchScheduled().then(data => setItems(data))
    }, [])

    return <Table dataSource={items} columns={columns} />
}

export default ScheduledTaskList
