package club.mcsports.droplet.queue

data class QueueType(
    val name: String,
    val group: String,
    val maxCapacity: Long,
    val minCapacity: Long,
)