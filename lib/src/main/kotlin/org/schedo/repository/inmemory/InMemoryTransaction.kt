package org.schedo.repository.inmemory

import org.schedo.repository.TransactionManager

class InMemoryTransaction : TransactionManager {
    override fun <T> transaction(block: () -> T): T =
        block()
}