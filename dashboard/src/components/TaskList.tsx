import { Table } from "antd";
import { useEffect, useState } from "react";
import {Link} from "react-router";

// matches TaskInfo from SchedoServer
export type Task = {
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
        render: (text: string) => <Link to={`/tasks/${text}`}>{text}</Link>
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

export async function fetchTasks(): Promise<Task[]> {
    const response = await fetch('http://localhost:8080//tasks')
    return response.json()
}

/**
 * Props:
 *   n?: number — how many tasks to show (default: all)
 */
function TaskList({ n = Infinity }: { n?: number }) {

    const [tasks, setTasks] = useState<Task[]>([])

    useEffect(() => {
        fetchTasks().then((tasks) => setTasks(tasks))
    }, [])

    const displayedTasks = tasks.slice(0, n)

    return <Table dataSource={displayedTasks} columns={columns} />;
}

export default TaskList;