package club.mcsports.droplet.queue.api.impl.coroutine

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.api.InteractionApi
import com.mcsports.queue.v1.QueueInteractionGrpcKt
import com.mcsports.queue.v1.dequeueRequest
import com.mcsports.queue.v1.enqueueRequest
import io.grpc.ManagedChannel
import java.util.*

class QueueInteractionApiCoroutineImpl(
    credentials: AuthCallCredentials,
    channel: ManagedChannel,
    private val dataApi: QueueDataApiCoroutineImpl,
) : InteractionApi.Coroutine {

    private val api = QueueInteractionGrpcKt.QueueInteractionCoroutineStub(channel).withCallCredentials(credentials)

    override suspend fun enqueue(type: String, vararg players: UUID): Queue? {
        return dataApi.getQueue(api.enqueue(enqueueRequest {
            queueName = type
            playerIds.addAll(players.toList().map { it.toString() })
        }).queueId)
    }

    override suspend fun dequeue(vararg players: UUID, force: Boolean): Boolean {
        try {
            api.dequeue(dequeueRequest {
                playerIds.addAll(players.toList().map { it.toString() })
                forced = force
            })
            return true
        } catch (_: Exception) {
            return false
        }
    }
}