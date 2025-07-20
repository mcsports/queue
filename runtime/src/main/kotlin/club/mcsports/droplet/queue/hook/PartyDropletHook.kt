package club.mcsports.droplet.queue.hook

import app.simplecloud.plugin.api.shared.extension.text
import club.mcsports.droplet.party.api.PartyApi
import club.mcsports.droplet.party.shared.extension.getMember
import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.extension.fetchPlayer
import club.mcsports.droplet.queue.extension.log
import com.mcsports.party.v1.PartyRole
import io.grpc.Status
import io.grpc.StatusException
import org.apache.logging.log4j.LogManager
import java.util.*

class PartyDropletHook(authSecret: String) {
    private val logger = LogManager.getLogger(PartyDropletHook::class.java)

    val api = try {
        PartyApi.createCoroutineApi(
            authSecret,
            System.getenv("PARTY_HOST") ?: "0.0.0.0",
            System.getenv("PARTY_PORT")?.toInt() ?: 5831
        )
    } catch (exception: Exception) {
        if(exception is ClassNotFoundException) {
            logger.warn("Failed to load party api: Couldn't find api classes. No party features will be considered")
        } else logger.error(exception.stackTraceToString())
        null
    }

    suspend fun queueWithParty(enqueueUuid: UUID): Set<String> {
        val party = try {
            api?.getData()?.getParty(enqueueUuid)
        } catch (exception: StatusException) {
            if (exception.status.code == Status.Code.NOT_FOUND) return setOf()

            throw exception.status.log(logger).asRuntimeException()
        }
        val enqueuePlayer = enqueueUuid.fetchPlayer()
        val enqueueMember = party?.getMember(enqueueUuid) ?: return setOf()

        if (enqueueMember.role != PartyRole.OWNER) {
            enqueuePlayer.sendMessage(text("${Color.RED} You must be the party owner in order to enqueue."))
            throw Status.PERMISSION_DENIED.withDescription("Failed to enqueue: ${enqueueMember.name} isn't the party owner")
                .log(logger).asRuntimeException()
        }

        return party.membersList.map { it.uuid }.toSet()
    }

}