import './App.css'
import { Outlet } from 'react-router'
import NavMenu from './components/NavMenu'

function App() {
  return (
      <>
        <NavMenu />
        <Outlet />
      </>
  )
}

export default App
