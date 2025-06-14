package org.schedo.repository.postgres

import org.schedo.repository.TransactionManager
import java.sql.Connection
import javax.sql.DataSource

class DataSourceTransaction(private val dataSource: DataSource) : TransactionManager {
    private val threadLocalConnection = ThreadLocal<Connection>()

    fun getConnection(): Connection {
        return threadLocalConnection.get()
            ?: throw IllegalStateException("No transaction is active on this thread")
    }

    override fun <T> transaction(block: () -> T): T {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            threadLocalConnection.set(conn) // store in thread-local
            try {
                val result = block()
                conn.commit()
                return result
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                // restore default
                threadLocalConnection.remove()
                conn.autoCommit = true
            }
        }
    }
}