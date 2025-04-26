package club.mcsports.droplet.queue

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class QueueType(
    val name: String = "",
    val group: String = "",
    val maxCapacity: Long = -1L,
    val minCapacity: Long = -1L,
    val messages: Messages = defaultMessages,
)