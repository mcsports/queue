package club.mcsports.droplet.queue.visualizer

import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.QueueType
import club.mcsports.droplet.queue.reconciler.InternalState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

interface QueueVisualizer {
    suspend fun send(queue: Queue, type: QueueType, internalState: InternalState)

    companion object {

        fun createQueueText(
            queue: Queue,
            type: QueueType,
            state: InternalState,
        ): Component {
            val raw = type.messages[state] ?: return Component.empty()
            val serverTagResolver = getServerTagResolver(queue)
            val queueTagResolver = QueueTagResolver.get(queue, type)

            return MiniMessage.builder().editTags { tags ->
                if (serverTagResolver != null)
                    tags.resolver(serverTagResolver)
                tags.resolver(queueTagResolver)
            }.build().deserialize(raw)
        }

        private fun getServerTagResolver(queue: Queue): TagResolver? {
            val server = queue.server ?: return null
            return ServerTagResolver.get(server)
        }
    }
}