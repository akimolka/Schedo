import { useParams } from 'react-router'
import TaskHistoryList from "../components/TaskHistoryList"

function TaskHistory() {
    const { taskName } = useParams()
    return (
        <>
            <h1>History of task: {taskName}</h1>
            <TaskHistoryList />
        </>
    )
}

export default TaskHistory