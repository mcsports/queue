package club.mcsports.droplet.queue.service

import app.simplecloud.plugin.api.shared.extension.text
import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.extension.asUuid
import club.mcsports.droplet.queue.extension.fetchPlayer
import club.mcsports.droplet.queue.extension.log
import club.mcsports.droplet.queue.hook.PartyDropletHook
import com.mcsports.queue.v1.*
import io.grpc.Status
import org.apache.logging.log4j.LogManager
import java.util.*

class QueueInteractionService(
    private val queues: QueueRepository,
    private val partyDropletHook: PartyDropletHook,
) : QueueInteractionGrpcKt.QueueInteractionCoroutineImplBase() {
    private val logger = LogManager.getLogger(QueueInteractionService::class.java)

    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val tempPlayerIds = request.playerIdsList.toMutableSet()
        if (request.playerIdsList.size == 1) tempPlayerIds.addAll(partyDropletHook.queueWithParty(request.playerIdsList.first().asUuid()))

        val queue = queues.enqueue(request.queueName, tempPlayerIds.map { UUID.fromString(it) })
        tempPlayerIds.forEach { uuid ->
            uuid.fetchPlayer()
                .sendMessage(text("<white>You ${Color.GREEN}successfully</color> enqueued for ${request.queueName}."))
        }

        return enqueueResponse {
            queueId = queue.id.toString()
        }
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        if (!queues.dequeue(request.playerIdsList.map { UUID.fromString(it) })) throw Status.INVALID_ARGUMENT.withDescription(
            "Failed to dequeue: Might not be in queue"
        ).log(logger).asRuntimeException()
        return dequeueResponse { }
    }
}