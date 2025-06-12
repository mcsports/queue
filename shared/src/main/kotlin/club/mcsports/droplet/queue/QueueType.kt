package club.mcsports.droplet.queue

import com.mcsports.queue.v1.QueueType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val group: String = "",
    val maxCapacity: Long = -1L,
    val minCapacity: Long = -1L,
    val messages: Messages = defaultMessages,
) {

    fun toDefinition(): QueueType {
        return QueueType.newBuilder()
            .setName(name)
            .setGroup(group)
            .setMaxCapacity(maxCapacity.toInt())
            .setMinCapacity(minCapacity.toInt())
            .build()
    }

    companion object {

        @JvmStatic
        fun fromDefinition(type: QueueType): club.mcsports.droplet.queue.QueueType {
            return QueueType(type.name, type.group, type.maxCapacity.toLong(), type.minCapacity.toLong())
        }
    }
}