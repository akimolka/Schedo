import './App.css'
import { Link, Outlet } from 'react-router'

function App() {
  return (
    <>
      <nav>
        <ul>
          <li>
            <Link to="/">Домой</Link>
          </li>
          <li>
            <Link to="/tasks">Задачи</Link>
          </li>
          <li>
            <Link to="/about">Информация</Link>
          </li>
        </ul>
      </nav>
      <Outlet />
    </>)
}

export default App
