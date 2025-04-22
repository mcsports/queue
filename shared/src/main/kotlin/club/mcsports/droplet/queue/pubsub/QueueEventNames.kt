package club.mcsports.droplet.queue.pubsub

object QueueEventNames {
    private const val PREFIX = "queue:"
    const val ENQUEUE = PREFIX + "enqueue"
    const val DEQUEUE = PREFIX + "dequeue"
    const val UPDATE = PREFIX + "update"
}