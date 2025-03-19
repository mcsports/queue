package club.mcsports.droplet.queue

import com.mcsports.queue.v1.QueueStatus
import java.util.*

data class Queue(
        val id: UUID,
        val type: String,
        var status: QueueStatus,
        val players: MutableList<UUID>,
        val capacity: Long = 0,
        var countdownMillis: Long? = null,
) {

    fun toDefinition(): com.mcsports.queue.v1.Queue {
        val builder = com.mcsports.queue.v1.Queue.newBuilder()
                .setUniqueId(id.toString())
                .setStatus(status)
                .setType(type)
                .addAllPlayerIds(players.map { it.toString() })
        if (countdownMillis != null) builder.setCountdownMillis(countdownMillis!!)
        return builder.build()
    }

    companion object {
        fun fromDefinition(definition: com.mcsports.queue.v1.Queue): Queue {
            return Queue(
                    id = UUID.fromString(definition.uniqueId),
                    type = definition.type,
                    status = definition.status,
                    players = definition.playerIdsList.map { UUID.fromString(it) }.toMutableList(),
                    countdownMillis = if (definition.hasCountdownMillis()) definition.countdownMillis else null
            )
        }
    }

}
