import { Menu } from 'antd'
import { Link, useLocation } from 'react-router'

const items = [
    {
        label: <Link to="/">Домой</Link>,
        key: "/",
    },
    {
        label: <Link to="/tasks">All Tasks</Link>,
        key: "/tasks",
    },
    {
        label: <Link to="/tasks/scheduled">Scheduled</Link>,
        key: "/tasks/scheduled",
    },
    {
        label: <Link to="/tasks/failed">Failed</Link>,
        key: "/tasks/failed",
    },
    {
        label: <Link to="/about">Информация</Link>,
        key: "/about",
    },
]

function NavMenu() {
    const location = useLocation();
    const selectedKey = items.find((item) => item && item.key === location.pathname)?.key || "/"

    return (
        <Menu
            mode="horizontal"
            selectedKeys={[selectedKey as string]}
            items={items}
        />
    )
}

export default NavMenu