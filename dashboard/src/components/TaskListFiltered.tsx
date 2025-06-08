import {useEffect, useState} from "react"
import {Table, Input, Space, Button, Select, DatePicker} from "antd"
import type {TableColumnsType} from "antd"
import {type Task, fetchTasks} from "./TaskList.tsx"
import {Link} from "react-router"

const { RangePicker } = DatePicker

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
            if (!a.lastExecutionTime && !b.lastExecutionTime) return 0
            if (!a.lastExecutionTime) return -1
            if (!b.lastExecutionTime) return 1
            const da = new Date(a.lastExecutionTime).getTime()
            const db = new Date(b.lastExecutionTime).getTime()
            return da - db
        },
    },
];

// Determines if a task's name contains the filter substring
function matchesName(task: Task, nameFilter: string): boolean {
    return task.name.toLowerCase().includes(nameFilter.trim().toLowerCase())
}

// Outcome filter constants
const OutcomeFilter = {
    NoExecutions: 'noExecutions',  // Successes = 0, Failures = 0
    SuccessfulOnly: 'successfulOnly',  // Successes > 0, Failures = 0
    FailedOnly: 'failedOnly',  // Successes = 0, Failures > 0
    MultipleSuccess: 'multipleSuccess',  // Successes > 1
} as const

type OutcomeFilterType = typeof OutcomeFilter[keyof typeof OutcomeFilter] | ''

const outcomeOptions = [
    { label: 'No Executions', value: OutcomeFilter.NoExecutions },
    { label: 'Successful Only', value: OutcomeFilter.SuccessfulOnly },
    { label: 'Failed Only', value: OutcomeFilter.FailedOnly },
    { label: 'Multiple Successes', value: OutcomeFilter.MultipleSuccess },
]

// Determines if a task matches the selected outcome filter
function matchesOutcome(task: Task, filter: OutcomeFilterType): boolean {
    switch (filter) {
        case OutcomeFilter.NoExecutions:
            return task.successCount === 0 && task.failureCount === 0
        case OutcomeFilter.SuccessfulOnly:
            return task.successCount > 0 && task.failureCount === 0
        case OutcomeFilter.FailedOnly:
            return task.successCount === 0 && task.failureCount > 0
        case OutcomeFilter.MultipleSuccess:
            return task.successCount > 1
        default:
            return true
    }
}

// Determines if a task's timestamp is within the range
function matchesTime(task: Task, timeRange: [Date, Date] | null): boolean {
    if (!timeRange) return true
    if (!task.lastExecutionTime) return false
    const [start, end] = timeRange
    const taskTime = new Date(task.lastExecutionTime)
    return taskTime >= start && taskTime <= end
}

function TaskListFiltered() {

    const [tasks, setTasks] = useState<Task[]>([])

    // Filter states
    const [nameFilter, setNameFilter] = useState<string>('')
    const [outcomeFilter, setOutcomeFilter] = useState<OutcomeFilterType>('')
    const [timeRange, setTimeRange] = useState<[Date, Date] | null>(null)

    useEffect(() => {
        fetchTasks().then((tasks) => setTasks(tasks))
    }, [])

    // Apply all filters
    const filteredTasks = tasks.filter((task) => {
        return (
            matchesName(task, nameFilter) &&
            matchesOutcome(task, outcomeFilter) &&
            matchesTime(task, timeRange)
        )
    })

    // Reset filters to defaults
    const resetFilters = () => {
        setNameFilter('')
        setOutcomeFilter('')
        setTimeRange(null)
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

                {/* Outcome filter */}
                <Select
                    placeholder="Filter by Outcome"
                    options={outcomeOptions}
                    value={outcomeFilter || undefined}
                    onChange={(value) => setOutcomeFilter(value)}
                    style={{ width: 180 }}
                    allowClear
                />

                {/* Last Execution Time range filter */}
                <RangePicker
                    showTime
                    placeholder={['From', 'To']}
                    onChange={(ranges) => {
                        if (ranges && ranges.length === 2 && ranges[0] && ranges[1]) {
                            setTimeRange([ranges[0].toDate(), ranges[1].toDate()])
                        } else {
                            setTimeRange(null)
                        }
                    }}
                />

                <Button onClick={resetFilters}>Reset Filters</Button>
            </Space>

            <Table dataSource={filteredTasks} columns={columns} rowKey="name" showSorterTooltip={{ target: 'sorter-icon' }} />
        </>
    )
}

export default TaskListFiltered;