package club.mcsports.droplet.queue.service

import app.simplecloud.plugin.api.shared.extension.text
import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.extension.fetchPlayer
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
import java.util.UUID

class QueueInteractionService(
    private val queues: QueueRepository,
) : QueueInteractionGrpcKt.QueueInteractionCoroutineImplBase() {
    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val tempPlayerIds = request.playerIdsList.toMutableSet()

        if(request.playerIdsList.size == 1) {
            PartyDropletHook.api?.let { api ->
                val enqueueUuid = UUID.fromString(request.playerIdsList.first())
                val party = api.getData().getParty(enqueueUuid)

                val enqueueMember = party.membersList.firstOrNull { it.uuid == enqueueUuid.toString() } ?: run {
                    enqueueUuid.fetchPlayer().sendMessage(text("${Color.RED} Failed to fetch your party member data. Please call an administrator about this."))
                    throw Status.DATA_LOSS.withDescription("Can not enqueue. Error while fetching party member").asRuntimeException()
                }

                if(enqueueMember.role != PartyRole.OWNER) throw Status.PERMISSION_DENIED.withDescription("Can not enqueue. You're not the party owner.")
                    .asRuntimeException()

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
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        if (!queues.dequeue(request.playerIdsList.map { UUID.fromString(it) })) throw Status.INVALID_ARGUMENT.withDescription(
            "Can not dequeue. Might not be in queue."
        ).asRuntimeException()
        return dequeueResponse { }
    }
}