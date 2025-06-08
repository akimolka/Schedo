import TaskList from "../components/TaskList.tsx";

function Home() {
    return (<>
        <h1>Task scheduler Schedo</h1>
        <TaskList n={5} />
    </>)
}

export default Home;