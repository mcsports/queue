package club.mcsports.droplet.queue.service

import app.simplecloud.plugin.api.shared.extension.text
import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.extension.fetchPlayer
import club.mcsports.droplet.queue.extension.log
import club.mcsports.droplet.queue.hook.PartyDropletHook
import com.mcsports.party.v1.PartyRole
import com.mcsports.queue.v1.DequeueRequest
import com.mcsports.queue.v1.DequeueResponse
import com.mcsports.queue.v1.EnqueueRequest
import com.mcsports.queue.v1.EnqueueResponse
import com.mcsports.queue.v1.QueueInteractionGrpcKt
import com.mcsports.queue.v1.dequeueResponse
import com.mcsports.queue.v1.enqueueResponse
import io.grpc.Status
import org.apache.logging.log4j.LogManager
import java.util.UUID

class QueueInteractionService(
    private val queues: QueueRepository,
) : QueueInteractionGrpcKt.QueueInteractionCoroutineImplBase() {
    private val logger = LogManager.getLogger(QueueInteractionService::class.java)

    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        try {
            val tempPlayerIds = request.playerIdsList.toMutableSet()

            if(request.playerIdsList.size == 1) {
                PartyDropletHook.api?.let { api ->
                    val enqueueUuid = UUID.fromString(request.playerIdsList.first())
                    val party = api.getData().getParty(enqueueUuid)
                    val enqueuePlayer = enqueueUuid.fetchPlayer()

                    val enqueueMember = party.membersList.firstOrNull { it.uuid == enqueueUuid.toString() } ?: run {
                        enqueuePlayer.sendMessage(text("${Color.RED} Failed to fetch your party member data. Please call an administrator about this."))
                        throw Status.DATA_LOSS.withDescription("Failed to enqueue: Error while fetching party member").log(logger).asRuntimeException()
                    }

                    if(enqueueMember.role != PartyRole.OWNER) {
                        enqueuePlayer.sendMessage(text("${Color.RED} You must be the party owner in order to enqueue."))
                        throw Status.PERMISSION_DENIED.withDescription("Failed to enqueue: ${enqueueMember.name} isn't the party owner")
                            .log(logger).asRuntimeException()
                    }

                    tempPlayerIds.addAll(party.membersList.map { it.uuid })
                }
            }

            val queue = queues.enqueue(request.queueName, tempPlayerIds.map { UUID.fromString(it) })
            tempPlayerIds.forEach { uuid ->
                uuid.fetchPlayer().sendMessage(text("<white>You ${Color.GREEN}successfully</color> enqueued for ${request.queueName}."))
            }

            return enqueueResponse {
                queueId = queue.id.toString()
            }
        } catch (exception: Exception) {
//            logger.warn(exception.stackTraceToString())
            throw exception
        }
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        if (!queues.dequeue(request.playerIdsList.map { UUID.fromString(it) })) throw Status.INVALID_ARGUMENT.withDescription(
            "Failed to dequeue: Might not be in queue"
        ).log(logger).asRuntimeException()
        return dequeueResponse { }
    }
}