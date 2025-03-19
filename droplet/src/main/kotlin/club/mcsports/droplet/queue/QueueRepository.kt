package club.mcsports.droplet.queue

import com.mcsports.queue.v1.QueueStatus
import java.util.*

class QueueRepository(
        private val types: QueueTypeRepository
) {

    private val playersToQueue = mutableMapOf<UUID, UUID>()
    private val queues = mutableMapOf<UUID, Queue>()

    fun getQueueByPlayer(playerId: UUID): Queue? {
        return playersToQueue.firstNotNullOfOrNull { if (it.key == playerId) it.value else null }?.let { queues[it] }
    }

    fun deleteQueue(queueId: UUID): Boolean {
        if (!queues.containsKey(queueId)) return false
        queues.remove(queueId)
        playersToQueue.filter { it.value == queueId }.forEach { playersToQueue.remove(it.key) }
        return true
    }

    fun OnEnqueue(queue: Queue): Queue {
        val type = types.get(queue.type)!!
        if (queue.status == QueueStatus.NOT_ENOUGH_PLAYERS) {
            if (queue.players.size >= type.minCapacity)
                queue.status = QueueStatus.SEARCHING_SERVER
        }
        return queue
    }

    fun OnDequeue(queue: Queue): Queue {
        val type = types.get(queue.type)!!
        if (queue.status != QueueStatus.NOT_ENOUGH_PLAYERS) {
            if (queue.players.size < type.minCapacity)
                queue.status = QueueStatus.NOT_ENOUGH_PLAYERS
        }
        return queue
    }

    fun enqueue(queueType: String, playerIds: List<UUID>): Queue? {
        val type = types.get(queueType) ?: return null
        if (playerIds.any { playersToQueue.containsKey(it) }) return null
        var queue = findQueue(queueType, playerIds.size) ?: createQueue(type)
        queue.players.addAll(playerIds)
        queue = OnEnqueue(queue)
        queues[queue.id] = queue
        playerIds.forEach { playersToQueue[it] = queue.id }
        return queue
    }

    private fun createQueue(type: QueueType): Queue {
        val queue = Queue(
                id = UUID.randomUUID(),
                type = type.name,
                capacity = type.maxCapacity,
                players = mutableListOf(),
                status = QueueStatus.NOT_ENOUGH_PLAYERS,
        )
        queues[queue.id] = queue
        return queue
    }

    fun dequeue(playerId: UUID): Boolean {
        if (!playersToQueue.containsKey(playerId)) return false
        var queue = getQueueByPlayer(playerId) ?: return false
        if (!playersToQueue.remove(playerId, queue.id)) return false
        if (!queue.players.remove(playerId)) {
            playersToQueue[playerId] = queue.id
            return false
        }
        queue = OnDequeue(queue)
        queues[queue.id] = queue
        return true
    }

    fun dequeue(playerIds: List<UUID>): Boolean {
        return !playerIds.any { !dequeue(it) }
    }

    fun getAllQueues(): List<Queue> {
        return queues.values.toList()
    }

    fun getQueue(queueId: UUID): Queue? {
        return queues[queueId]
    }

    private fun findQueue(queueType: String, playerAmount: Int): Queue? {
        return queues.values.firstOrNull { it.type == queueType && playerAmount + it.players.size <= it.capacity }
    }

}