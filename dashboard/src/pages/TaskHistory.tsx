import { useParams } from 'react-router'
import TaskHistoryTable from "../components/TaskHistoryTable.tsx"

function TaskHistory() {
    const { taskName } = useParams()
    return (
        <>
            <h1>History of task: {taskName}</h1>
            <TaskHistoryTable />
        </>
    )
}

export default TaskHistory