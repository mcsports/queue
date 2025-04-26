package club.mcsports.droplet.queue.visualizer

import app.simplecloud.droplet.player.api.PlayerApi
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.QueueType
import club.mcsports.droplet.queue.reconciler.InternalState

class ActionbarVisualizer(
    private val playerApi: PlayerApi.Coroutine
) : QueueVisualizer {
    override suspend fun send(
        queue: Queue, type: QueueType, internalState: InternalState
    ) {
        queue.players.forEach { player ->
            playerApi.getOnlinePlayer(player).sendActionBar(QueueVisualizer.createQueueText(queue, type, internalState))
        }
    }

}