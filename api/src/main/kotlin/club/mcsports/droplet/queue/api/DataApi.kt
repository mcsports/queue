package club.mcsports.droplet.queue.api

import club.mcsports.droplet.queue.Queue
import java.util.concurrent.CompletableFuture

interface DataApi {
    interface Coroutine {
        suspend fun getAllQueues(): List<Queue>
        suspend fun getQueue(id: String): Queue?
        suspend fun updateQueue(queue: Queue): Queue?
        suspend fun deleteQueue(id: String): Boolean
    }

    interface Future {
        fun getAllQueues(): CompletableFuture<List<Queue>>
        fun getQueue(id: String): CompletableFuture<Queue?>
        fun updateQueue(queue: Queue): CompletableFuture<Queue?>
        fun deleteQueue(id: String): CompletableFuture<Boolean>
    }
}