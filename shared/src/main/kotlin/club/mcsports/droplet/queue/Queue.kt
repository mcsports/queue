package club.mcsports.droplet.queue

import com.mcsports.queue.v1.QueueStatus
import java.util.*

data class Queue(
        val id: UUID,
        val type: String,
        val status: QueueStatus,
        val players: MutableList<UUID>,
) {

    fun toDefinition(): com.mcsports.queue.v1.Queue {
        return com.mcsports.queue.v1.Queue.newBuilder()
                .setUniqueId(id.toString())
                .setStatus(status)
                .addAllPlayerIds(players.map { it.toString() })
                .build()
    }

    companion object {
        fun fromDefinition(definition: com.mcsports.queue.v1.Queue): Queue {
            return Queue(
                    id = UUID.fromString(definition.uniqueId),
                    type = "",
                    status = definition.status,
                    players = definition.playerIdsList.map { UUID.fromString(it) }.toMutableList()
            )
        }
    }

}
