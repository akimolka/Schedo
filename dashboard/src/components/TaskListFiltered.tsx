import {useEffect, useState} from "react";
import {Table, Input, InputNumber, Space, Button, type TableColumnsType} from "antd";
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

    // Filter states
    const [nameFilter, setNameFilter] = useState<string>('')
    const [successMin, setSuccessMin] = useState<number>(0)
    const [successMax, setSuccessMax] = useState<number>(Infinity)

    useEffect(() => {
        fetchTasks().then((tasks) => setTasks(tasks))
    }, [])

    // Filter logic
    const filteredTasks = tasks.filter((task) => {
        // Filter by name substring (case-insensitive)
        const nameMatches = task.name.toLowerCase().includes(nameFilter.trim().toLowerCase())

        // Filter by successCount bounds
        const successOk =
            task.successCount >= successMin && task.successCount <= successMax

        return nameMatches && successOk
    })

    // Reset filters to defaults
    const resetFilters = () => {
        setNameFilter('')
        setSuccessMin(0)
        setSuccessMax(Infinity)
    }

    return (
        <>
            <Space style={{ marginBottom: 16 }} wrap>
                {/* Name substring filter */}
                <Input
                    placeholder="Filter by Name"
                    value={nameFilter}
                    onChange={(e) => setNameFilter(e.target.value)}
                    style={{ width: 200 }}
                    allowClear
                />

                <p>Success:</p>

                {/* Success count lower bound */}
                <InputNumber
                    placeholder="min"
                    min={0}
                    value={successMin}
                    onChange={(value) => setSuccessMin(value !== null ? value : 0)}
                />

                {/* Success count upper bound */}
                <InputNumber
                    placeholder="max"
                    min={0}
                    value={successMax}
                    onChange={(value) => setSuccessMax(value !== null ? value : Infinity)}
                />

                <Button onClick={resetFilters}>Reset Filters</Button>
            </Space>

            <Table dataSource={filteredTasks} columns={columns} showSorterTooltip={{ target: 'sorter-icon' }} />
        </>
    )
}

export default TaskListFiltered;