import { Steps } from 'antd'
import { CheckCircleTwoTone, CloseCircleTwoTone, QuestionCircleTwoTone } from '@ant-design/icons'
import type { StatusEntry } from './TaskHistoryTable.tsx'

function formatTimestamp(timestamp?: string): string {
    if (!timestamp) return '-'
    const date = new Date(timestamp)
    const now = new Date()
    const isToday = date.getFullYear() === now.getFullYear() &&
        date.getMonth() === now.getMonth() &&
        date.getDate() === now.getDate()
    const pad = (n: number) => n.toString().padStart(2, '0')

    const hours = pad(date.getHours())
    const minutes = pad(date.getMinutes())
    const seconds = pad(date.getSeconds())
    if (isToday) {
        return `${hours}:${minutes}:${seconds}`
    }
    const year = date.getFullYear()
    const month = pad(date.getMonth() + 1)
    const day = pad(date.getDate())
    return `${year}-${month}-${day}\n${hours}:${minutes}:${seconds}`
}

const getFinishedIcon = ({ entry }: { entry: StatusEntry }) => {
    if (!entry.finishedAt) return null
    if (entry.status === 'COMPLETED') return <CheckCircleTwoTone twoToneColor="#52c41a" />
    if (entry.status === 'FAILED') return <CloseCircleTwoTone twoToneColor="#ff4d4f" />
    return <QuestionCircleTwoTone twoToneColor="#d9d9d9" />
}

function TaskHistorySteps({ entry }: { entry: StatusEntry }) {
  const steps = [
    { title: 'Scheduled', time: entry.scheduledAt },
    { title: 'Enqueued', time: entry.enqueuedAt },
    { title: 'Started', time: entry.startedAt },
    { title: 'Finished', time: entry.finishedAt }
  ]

    // Chooses last non-null.
    // Entry appears in StatusTable with scheduledAt != null, so all can't be null.
    // But if the situation happens all the same, Scheduled will be active step
  const activeStep = steps.reduce((acc, step, idx) => step.time ? idx : acc, 0)

  return (
    <Steps
      size="small"
      current={activeStep}
      items={steps.map((step, index) => {
          return {
              title: step.title,
              description: (
                  <div style={{
                      width: '100px',
                      textAlign: 'left',
                      padding: '0 auto',
                      margin: '0 auto',
                      whiteSpace: 'pre-wrap'
                  }}>
                      {formatTimestamp(step.time)}
                  </div>
              ),

              icon: index === 3 ? getFinishedIcon({entry}) : undefined
          }
      })}
    />
  )
}

export default TaskHistorySteps