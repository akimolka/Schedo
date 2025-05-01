package org.schedo.repository

import org.schedo.repository.inmemory.InMemoryTasks
import org.schedo.repository.postgres.PostgresStatusRepository
import org.schedo.repository.postgres.PostgresTasksRepository
import org.schedo.repository.inmemory.InMemoryStatus
import javax.sql.DataSource

/**
 * String name should be equal to conn.metaData.databaseProductName
 */
sealed class DataSourceType(val name: String) {
    data object Postgres   : DataSourceType("PostgreSQL")
    data class Other(val productName: String) : DataSourceType(productName)

    companion object {
        fun autodetect(dataSource: DataSource): DataSourceType {
            return dataSource.connection.use { conn ->
                when (val product = conn.metaData.databaseProductName) {
                    DataSourceType.Postgres.name  -> DataSourceType.Postgres
                    else                          -> DataSourceType.Other(product)
                }
            }
        }
    }
}