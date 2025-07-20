package club.mcsports.droplet.queue.service


import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.Glyphs
import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.extension.asUuid
import club.mcsports.droplet.queue.extension.fetchPlayer
import club.mcsports.droplet.queue.extension.log
import club.mcsports.droplet.queue.hook.PartyDropletHook
import com.mcsports.queue.v1.*
import io.grpc.Status
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.logging.log4j.LogManager
import java.util.*

class QueueInteractionService(
    private val queues: QueueRepository,
    private val partyDropletHook: PartyDropletHook,
) : QueueInteractionGrpcKt.QueueInteractionCoroutineImplBase() {
    private val logger = LogManager.getLogger(QueueInteractionService::class.java)

    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val tempPlayerIds = request.playerIdsList.toMutableSet()
        if (request.playerIdsList.size == 1) tempPlayerIds.addAll(
            partyDropletHook.queueWithParty(
                request.playerIdsList.first().asUuid()
            )
        )

        val queue = queues.enqueue(request.queueName, tempPlayerIds.map { UUID.fromString(it) })
        tempPlayerIds.forEach { uuid ->
            uuid.fetchPlayer()
                .sendMessage(
                    Glyphs.HOUR_GLASS.append(Component.text("You").color(NamedTextColor.WHITE).append(Component.text(" successfully ").color(Color.GREEN))
                        .append(Component.text("enqueued for ${request.queueName}.").color(NamedTextColor.WHITE))
                ))
        }

        return enqueueResponse {
            queueId = queue.id.toString()
        }
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        if (!queues.dequeue(request.playerIdsList.map { UUID.fromString(it) })) {

            request.playerIdsList.forEach { uuid ->
                uuid.fetchPlayer()
                    .sendMessage(Glyphs.HOUR_GLASS.append(Component.text("You aren't enqueued for any game.").color(Color.RED)))
            }

            throw Status.INVALID_ARGUMENT.withDescription(
                "Failed to dequeue: Might not be in queue"
            ).log(logger).asRuntimeException()
        }

        request.playerIdsList.forEach { uuid ->
            uuid.fetchPlayer()
                .sendMessage(Glyphs.HOUR_GLASS.append(Component.text("You successfully").color(NamedTextColor.WHITE).append(Component.text(" dequeued").color(Color.RED)).append(Component.text(".").color(NamedTextColor.WHITE))))
        }
        return dequeueResponse { }
    }
}