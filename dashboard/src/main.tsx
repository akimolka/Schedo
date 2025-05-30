import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { BrowserRouter, Route, Routes } from 'react-router'
import Home from './pages/Home.tsx'
import Tasks from './pages/Tasks.tsx'
import About from './pages/About.tsx'
import Scheduled from './pages/Scheduled.tsx'
import Failed from './pages/Failed.tsx'
import TaskHistory from './pages/TaskHistory.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />}>
          <Route index element={<Home />} />
          <Route path="tasks/scheduled" element={<Scheduled />} />
          <Route path="tasks/failed" element={<Failed />} />
          <Route path="tasks" element={<Tasks />} />
          <Route path="tasks/:taskName" element={<TaskHistory />} />
          <Route path="about" element={<About />} />
        </Route>
      </Routes>
    </BrowserRouter> </StrictMode>,
)
