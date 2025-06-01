import {useEffect, useState} from "react";
import {Table, type TableColumnsType} from "antd";
import {type Task, fetchTasks} from "./TaskList.tsx"
import {Link} from "react-router";

const columns: TableColumnsType<Task> = [
    {
        title: 'Name',
        dataIndex: 'name',
        key: 'name',
        render: (text: string) => <Link to={`/tasks/${text}`}>{text}</Link>,
        sorter: (a, b) => a.name.localeCompare(b.name),
    },
    {
        title: 'Success Count',
        dataIndex: 'successCount',
        key: 'successCount',
        sorter: (a, b) => a.successCount - b.successCount,
    },
    {
        title: 'Failure Count',
        dataIndex: 'failureCount',
        key: 'failuresCount',
        sorter: (a, b) => a.failureCount - b.failureCount,
    },
    {
        title: 'Last Execution Time',
        dataIndex: 'lastExecutionTime',
        key: 'lastExecutionTime',
        defaultSortOrder: 'descend',
        sorter: (a, b) => {
            const da = new Date(a.lastExecutionTime).getTime()
            const db = new Date(b.lastExecutionTime).getTime()
            return da - db
        },
    },
];

function TaskListFiltered() {

    const [tasks, setTasks] = useState<Task[]>([])

    useEffect(() => {
        fetchTasks().then((tasks) => setTasks(tasks))
    }, [])

    return <Table dataSource={tasks} columns={columns} showSorterTooltip={{ target: 'sorter-icon' }} />;
}

export default TaskListFiltered;