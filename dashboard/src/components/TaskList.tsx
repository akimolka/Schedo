import { Table } from "antd";
import { useEffect, useState } from "react";


type Task = {
    name: string,
    successCount: number,
    failureCount: number,
    lastExecutionTime: string
}

const columns = [
    {
        title: 'Name',
        dataIndex: 'name',
        key: 'name',
    },
    {
        title: 'Success Count',
        dataIndex: 'successCount',
        key: 'successCount',
    },
    {
        title: 'Failure Count',
        dataIndex: 'failureCount',
        key: 'failuresCount',
    },
    {
        title: 'Last Execution Time',
        dataIndex: 'lastExecutionTime',
        key: 'lastExecutionTime',
    },
];

async function fetchTasks(): Promise<Task[]> {
    const response = fetch('http://localhost:8080//tasks')

    return (await response).json()
}

function TaskList() {

    const [tasks, setTasks] = useState<Task[]>([])

    useEffect(() => {
        fetchTasks().then((tasks) => setTasks(tasks))
    }, [])

    return <Table dataSource={tasks} columns={columns} />;
}

export default TaskList;