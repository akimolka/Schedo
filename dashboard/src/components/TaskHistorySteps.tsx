import { Steps, Popover } from 'antd'
import { CheckCircleTwoTone, CloseCircleTwoTone, QuestionCircleTwoTone } from '@ant-design/icons'
import type { StatusEntry } from './TaskHistoryList'

// TODO for some reason it was "timestamp?: string | null"
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
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

function renderPopoverContent(info: { errorMessage?: string; stackTrace?: string }) {
    return (
        <div>
            {info.errorMessage && (
                <p><strong>Error:</strong> {info.errorMessage}</p>
            )}
            {info.stackTrace && (
                <p><strong>Stack trace:</strong> {info.stackTrace}</p>
            )}
            {!info.errorMessage && !info.stackTrace && (
                <p>No additional info</p>
            )}
        </div>
    )
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

  const getStatusIcon = () => {
    if (!entry.finishedAt) return null
    if (entry.status === 'COMPLETED') return <CheckCircleTwoTone twoToneColor="#52c41a" />
    if (entry.status === 'FAILED') return <CloseCircleTwoTone twoToneColor="#ff4d4f" />
    return <QuestionCircleTwoTone twoToneColor="#d9d9d9" />
  }

  return (
    <Steps
      size="small"
      current={activeStep}
      items={steps.map((step, index) => {
          let titleElement: React.ReactNode = step.title
          // Show popover on last step whenever entry.info is defined
          if (index === 3 && entry.info) {
              titleElement = (
                  <Popover content={renderPopoverContent(entry.info)} trigger="click">
                      <span style={{ cursor: 'pointer' }}>{step.title}</span>
                  </Popover>
              )
          }
          return {
              title: titleElement,
              description: formatTimestamp(step.time),
              icon: index === 3 ? getStatusIcon() : undefined
          }
      })}
    />
  )
}

export default TaskHistorySteps