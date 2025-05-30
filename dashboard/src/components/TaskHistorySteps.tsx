import { Steps } from 'antd'
import { CheckCircleTwoTone, CloseCircleTwoTone, QuestionCircleTwoTone } from '@ant-design/icons'
import type { StatusEntry } from './TaskHistoryList'

function TaskHistorySteps({ entry }: { entry: StatusEntry }) {
  const steps = [
    { title: 'Scheduled', time: entry.scheduledAt },
    { title: 'Enqueued', time: entry.enqueuedAt },
    { title: 'Started', time: entry.startedAt },
    { title: 'Finished', time: entry.finishedAt }
  ]

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
      items={steps.map((step, index) => ({
        title: step.title,
        description: step.time || '-',
        icon: index === 3 ? getStatusIcon() : undefined
      }))}
    />
  )
}

export default TaskHistorySteps