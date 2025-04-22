package club.mcsports.droplet.queue.reconciler

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.droplet.player.api.PlayerApi
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerUpdateEvent
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.QueueTypeRepository
import club.mcsports.droplet.queue.server.ServerFinder
import com.mcsports.queue.v1.QueueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * Reconciles queue statuses based on queue updates or server registrations.
 * Handles the state transitions of queues according to their current status.
 */
class QueueStatusReconciler(
    private val queues: QueueRepository,
    private val types: QueueTypeRepository,
    private val finder: ServerFinder,
    private val playerApi: PlayerApi.Coroutine,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Custom internal states for tracking queue progress beyond the basic QueueStatus enum
    private enum class InternalState {
        NOT_ENOUGH_PLAYERS,
        SEARCHING_SERVER,
        WAITING_FOR_SERVER,
        SERVER_READY,
        COUNTDOWN,
        TELEPORTING,
        FINISHED
    }

    // Map to track internal states for queues
    private val queueInternalStates = mutableMapOf<UUID, InternalState>()

    /**
     * Reconciles a queue's status based on its current state.
     * This method should be called when a queue is updated or a server is registered.
     *
     * @param queueId The ID of the queue to reconcile
     */
    suspend fun reconcile(queueId: UUID) {
        val queue = queues.getQueue(queueId) ?: return

        // Get or initialize internal state
        val internalState = queueInternalStates[queueId] ?: when (queue.status) {
            QueueStatus.NOT_ENOUGH_PLAYERS -> InternalState.NOT_ENOUGH_PLAYERS
            QueueStatus.SEARCHING_SERVER -> InternalState.SEARCHING_SERVER
            QueueStatus.UNRECOGNIZED -> InternalState.NOT_ENOUGH_PLAYERS
            else -> InternalState.NOT_ENOUGH_PLAYERS
        }

        // Store the internal state
        queueInternalStates[queueId] = internalState

        // Handle based on internal state
        when (internalState) {
            InternalState.NOT_ENOUGH_PLAYERS -> handleNotEnoughPlayers(queue)
            InternalState.SEARCHING_SERVER -> handleSearchingServer(queue)
            InternalState.WAITING_FOR_SERVER -> handleWaitingForServer(queue)
            InternalState.SERVER_READY -> handleServerReady(queue)
            InternalState.COUNTDOWN -> handleCountdown(queue)
            InternalState.TELEPORTING -> handleTeleporting(queue)
            InternalState.FINISHED -> handleFinished(queue)
        }
    }

    /**
     * Updates the internal state of a queue.
     */
    private fun updateInternalState(queueId: UUID, newState: InternalState) {
        queueInternalStates[queueId] = newState
    }

    /**
     * Handles a queue with NOT_ENOUGH_PLAYERS status.
     * Checks if there are enough players to start searching for a server.
     */
    private suspend fun handleNotEnoughPlayers(queue: Queue) {
        val type = types.get(queue.type) ?: return
        if (queue.players.size >= type.minCapacity) {
            updateInternalState(queue.id, InternalState.SEARCHING_SERVER)
            reconcile(queue.id)
        }
    }

    /**
     * Handles a queue with SEARCHING_SERVER status.
     * Attempts to find or request a server for the queue.
     */
    private suspend fun handleSearchingServer(queue: Queue) {
        val server = finder.reserveOrRequestServer(queue)
        if (server != null) {
            // Server found, update internal state
            updateInternalState(queue.id, InternalState.SERVER_READY)
            queue.server = server
            reconcile(queue.id)
            return
        }
        // No server found, a new one was requested, so we switch to the waiting state
        updateInternalState(queue.id, InternalState.WAITING_FOR_SERVER)
    }

    /**
     * Handles a queue with WAITING_FOR_SERVER status.
     * Checks if a server has become available for the queue.
     */
    private suspend fun handleWaitingForServer(queue: Queue) {
        // If we have a server finder, use it to find a server
        val server = finder.findServer(queue)
        if (server != null && server.state == ServerState.AVAILABLE) {
            updateInternalState(queue.id, InternalState.SERVER_READY)
            queue.server = server
            reconcile(queue.id)
        }
    }

    /**
     * Handles a queue with SERVER_READY status.
     * Starts the countdown for teleporting players.
     */
    private fun handleServerReady(queue: Queue) {
        // Start countdown
        updateInternalState(queue.id, InternalState.COUNTDOWN)
        queue.countdownMillis = 10.seconds.inWholeMilliseconds

        // Schedule countdown completion
        scope.launch {
            delay(10.seconds)
            val updatedQueue = queues.getQueue(queue.id)
            if (updatedQueue != null && queueInternalStates[updatedQueue.id] == InternalState.COUNTDOWN) {
                updateInternalState(updatedQueue.id, InternalState.TELEPORTING)
                reconcile(updatedQueue.id)
            }
        }
    }

    /**
     * Handles a queue with COUNTDOWN status.
     * Updates the countdown timer and transitions to TELEPORTING when done.
     */
    private fun handleCountdown(queue: Queue) {
        // In a real implementation, this would update the countdown timer
        // For now, we'll just assume it's handled by the scheduled task in handleServerReady
    }

    /**
     * Handles a queue with TELEPORTING status.
     * Teleports players to the server and marks the queue as finished.
     */
    private suspend fun handleTeleporting(queue: Queue) {
        val server = queue.server ?: return
        val serverId = "${server.group}-${server.numericalId}"
        queue.players.forEach { playerId ->
            playerApi.getOnlinePlayer(playerId).sendMessage(Component.text("Starting game on server $serverId..."))
            playerApi.connectPlayer(playerId, serverId)
        }
        updateInternalState(queue.id, InternalState.FINISHED)
        reconcile(queue.id)
    }

    /**
     * Handles a queue with FINISHED status.
     * Cleans up the queue.
     */
    private fun handleFinished(queue: Queue) {
        clear(queue)
    }

    /**
     * Reconciles all queues in the repository.
     * This can be called periodically to ensure all queues are in the correct state.
     */
    suspend fun reconcileAll() {
        queues.getAllQueues().forEach { queue ->
            reconcile(queue.id)
        }
    }

    /**
     * Handles server registration events.
     * Checks if any waiting queues can use the new server.
     *
     * @param server The newly registered server
     */
    suspend fun handleServerRegistration(server: Server) {
        // Find queues waiting for servers
        this@QueueStatusReconciler.queues.getAllQueues()
            .filter { queueInternalStates[it.id] == InternalState.WAITING_FOR_SERVER }
            .forEach { queue ->
                if (finder.reserveServer(queue, server)) {
                    updateInternalState(queue.id, InternalState.SERVER_READY)
                    reconcile(queue.id)
                    // Return early since we found a server for the queue
                    return
                }
            }
        // Free the server as we found no queues that match this server
        finder.freeServer(server)
    }

    fun registerServerRegistrationSubscriber(client: PubSubClient) {
        client.subscribe("event", ServerUpdateEvent::class.java) { event ->
            if (event.serverAfter.serverState != ServerState.AVAILABLE) return@subscribe
            if (event.serverBefore.serverState == event.serverAfter.serverState) return@subscribe
            scope.launch {
                handleServerRegistration(Server.fromDefinition(event.serverAfter))
            }
        }
    }

    fun clear(queue: Queue) {
        clear(queue.id)
    }

    fun clear(id: UUID) {
        queueInternalStates.remove(id)
    }

    fun startPeriodicReconciliation() {
        scope.launch {
            while (true) {
                reconcileAll()
                delay(30000) // 30 seconds
            }
        }
    }
}
