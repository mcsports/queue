package club.mcsports.droplet.queue.api.impl.future

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.future.toCompletable
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.api.InteractionApi
import com.mcsports.queue.v1.DequeueRequest
import com.mcsports.queue.v1.EnqueueRequest
import com.mcsports.queue.v1.QueueInteractionGrpc
import io.grpc.ManagedChannel
import java.util.*
import java.util.concurrent.CompletableFuture

class QueueInteractionApiFutureImpl(
    channel: ManagedChannel,
    credentials: AuthCallCredentials,
    private val dataApi: QueueDataApiFutureImpl,
) : InteractionApi.Future {

    private val api = QueueInteractionGrpc.newFutureStub(channel).withCallCredentials(credentials)
    override fun enqueue(
        type: String, vararg players: UUID
    ): CompletableFuture<Queue?> {
        return api.enqueue(
            EnqueueRequest.newBuilder().setQueueName(type).addAllPlayerIds(players.map { it.toString() }).build()
        ).toCompletable().thenApply {
            return@thenApply try {
                dataApi.getQueue(it.queueId).get()
            } catch (_: Exception) {
                null
            }
        }.exceptionally { null }
    }

    override fun dequeue(vararg players: UUID, force: Boolean): CompletableFuture<Boolean> {
        return api.dequeue(
            DequeueRequest.newBuilder().addAllPlayerIds(players.map { it.toString() }).setForced(force).build()
        ).toCompletable().thenApply { true }.exceptionally { false }
    }
}