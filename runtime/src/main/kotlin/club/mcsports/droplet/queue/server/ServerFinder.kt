package club.mcsports.droplet.queue.server

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.api.dsl.extensions.start
import app.simplecloud.controller.api.dsl.extensions.updateProperties
import app.simplecloud.controller.shared.server.Server
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.QueueTypeRepository
import org.apache.logging.log4j.LogManager

class ServerFinder(
    private val api: ControllerApi.Coroutine,
    private val types: QueueTypeRepository,
) {
    companion object {
        private val logger = LogManager.getLogger(ServerFinder::class.java)
    }


    suspend fun findServer(queue: Queue): Server? {
        val type = types.get(queue.type) ?: return null
        api.getServers().getServersByGroup(type.group).firstOrNull {
            it.properties["queue-id"] == queue.id.toString()
        }.let {
            return it
        }
    }

    suspend fun freeServer(server: Server): Boolean {
        try {
            api.getServers().updateProperties(server.uniqueId) {
                "queue-id" to null
            }
            return true
        } catch (e: Exception) {
            logger.error("Failed to free server ${server.uniqueId}", e)
            return false
        }

    }

    suspend fun reserveOrRequestServer(queue: Queue): Server? {
        val reserved = reserveServer(queue)
        if (reserved != null) {
            return reserved
        }
        requestNewServer(queue)
        return null
    }

    suspend fun reserveServer(queue: Queue): Server? {
        val type = types.get(queue.type) ?: return null
        val server = api.getServers().getServersByGroup(type.group).firstOrNull {
            canReserveServer(queue, it)
        } ?: return null
        api.getServers().updateServerProperty(server.uniqueId, "queue-id", queue.id.toString())
        queue.server = server
        return server
    }

    fun canReserveServer(queue: Queue, server: Server): Boolean {
        val type = types.get(queue.type) ?: return false
        return type.group == server.group && (!server.properties.containsKey("queue-id") || server.properties["queue-id"] == null || server.properties["queue-id"] == queue.id.toString())
    }

    suspend fun reserveServer(queue: Queue, server: Server): Boolean {
        if (!canReserveServer(queue, server)) return false
        api.getServers().updateServerProperty(server.uniqueId, "queue-id", queue.id.toString())
        queue.server = server
        return true
    }

    suspend fun requestNewServer(queue: Queue): Server? {
        val type = types.get(queue.type) ?: return null
        val result = try {
            api.getServers().start(type.group) {
                "queue-id" to queue.id.toString()
            }
        } catch (e: Exception) {
            logger.error("Failed to request new server for ${queue.type}", e)
            null
        }
        if (result != null) {
            queue.server = result
        }
        return result
    }

}