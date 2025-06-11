package club.mcsports.droplet.queue.api

import club.mcsports.droplet.queue.Queue
import com.mcsports.queue.v1.GetQueueTypePlayerInformationResponse
import com.mcsports.queue.v1.QueueType
import java.util.*
import java.util.concurrent.CompletableFuture

interface DataApi {
    interface Coroutine {
        suspend fun getAllQueues(): List<Queue>
        suspend fun getQueue(id: String): Queue?
        suspend fun updateQueue(queue: Queue): Queue?
        suspend fun deleteQueue(id: String): Boolean
        suspend fun getAllQueueTypes(): List<QueueType>
        suspend fun getQueueByPlayer(player: UUID): Queue?
        suspend fun getQueueTypePlayerInformation(type: String): GetQueueTypePlayerInformationResponse
    }

    interface Future {
        fun getAllQueues(): CompletableFuture<List<Queue>>
        fun getQueue(id: String): CompletableFuture<Queue?>
        fun updateQueue(queue: Queue): CompletableFuture<Queue?>
        fun deleteQueue(id: String): CompletableFuture<Boolean>
        fun getAllQueueTypes(): CompletableFuture<List<QueueType>>
        fun getQueueByPlayer(player: UUID): CompletableFuture<Queue?>
        fun getQueueTypePlayerInformation(type: String): CompletableFuture<GetQueueTypePlayerInformationResponse>
    }
}