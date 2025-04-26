package club.mcsports.droplet.queue.visualizer

import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.QueueType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object QueueTagResolver {

    fun get(queue: Queue, type: QueueType): TagResolver {
        return TagResolver.resolver(
            TagResolver.resolver("queue_type", Tag.inserting(Component.text(queue.type))),
            TagResolver.resolver("queue_id", Tag.inserting(Component.text(queue.id.toString()))),
            TagResolver.resolver("queue_players", Tag.inserting(Component.text(queue.players.size.toString()))),
            TagResolver.resolver("queue_max_capacity", Tag.inserting(Component.text(type.maxCapacity.toString()))),
            TagResolver.resolver("queue_min_capacity", Tag.inserting(Component.text(type.minCapacity.toString()))),
            TagResolver.resolver(
                "queue_countdown_seconds",
                Tag.inserting(Component.text(queue.countdownMillis?.div(1000).toString()))
            ),
        )
    }
}