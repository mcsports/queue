package club.mcsports.droplet.queue.api

import club.mcsports.droplet.queue.Queue
import java.util.*
import java.util.concurrent.CompletableFuture

interface InteractionApi {
    interface Coroutine {
        suspend fun enqueue(type: String, vararg players: UUID): Queue?
        suspend fun dequeue(vararg players: UUID, force: Boolean = false): Boolean
    }

    interface Future {
        fun enqueue(type: String, vararg players: UUID): CompletableFuture<Queue?>
        fun dequeue(vararg players: UUID, force: Boolean = false): CompletableFuture<Boolean>
    }
}