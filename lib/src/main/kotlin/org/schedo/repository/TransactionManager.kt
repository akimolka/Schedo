package org.schedo.repository

interface TransactionManager {
    fun <T> transaction(block: () -> T): T
}