package club.mcsports.droplet.queue.api.impl.coroutine

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.api.DataApi
import com.mcsports.queue.v1.*
import io.grpc.ManagedChannel
import java.util.*

class QueueDataApiCoroutineImpl(
    credentials: AuthCallCredentials,
    channel: ManagedChannel,
) : DataApi.Coroutine {
    private val api = QueueDataGrpcKt.QueueDataCoroutineStub(channel).withCallCredentials(credentials)

    override suspend fun getAllQueues(): List<Queue> {
        return api.getAllQueues(getAllQueuesRequest { }).queuesList.map { Queue.fromDefinition(it) }
    }

    override suspend fun getQueue(id: String): Queue? {
        return api.getQueue(getQueueRequest {
            queueId = id
        }).queue?.let { Queue.fromDefinition(it) }
    }

    override suspend fun updateQueue(queue: Queue): Queue? {
        return api.updateQueue(updateQueueRequest {
            queueId = queue.id.toString()
            updated = queue.toDefinition()
        }).result?.let { Queue.fromDefinition(it) }
    }

    override suspend fun deleteQueue(id: String): Boolean {
        try {
            api.deleteQueue(deleteQueueRequest {
                this.queueId = id
            })
            return true
        } catch (_: Exception) {
            return false
        }
    }

    override suspend fun getAllQueueTypes(): List<QueueType> {
        return api.getAllQueueTypes(getAllQueueTypesRequest { }).typesList
    }

    override suspend fun getQueueByPlayer(player: UUID): Queue? {
        return api.getQueueByPlayer(getQueueByPlayerRequest {
            this.playerId = player.toString()
        }).result?.let { Queue.fromDefinition(it) }
    }

    override suspend fun getQueueTypePlayerInformation(type: String): GetQueueTypePlayerInformationResponse {
        return api.getQueueTypePlayerInformation(getQueueTypePlayerInformationRequest {
            this.type = type
        })
    }
}