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
import club.mcsports.droplet.queue.visualizer.QueueVisualizer
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
    private val visualizer: QueueVisualizer? = null,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Custom internal states for tracking queue progress beyond the basic QueueStatus enum
    // Map to track countdown start timestamps for delta time calculation
    private val countdownStartTimes = mutableMapOf<UUID, Long>()

    // Map to track last update time for delta time calculation
    private val lastUpdateTimes = mutableMapOf<UUID, Long>()

    // Map to track internal states for queues
    private val queueInternalStates = mutableMapOf<UUID, InternalState>()

    /**
     * Reconciles a queue's status based on its current state.
     * This method should be called when a queue is updated or a server is registered.
     *
     * @param queueId The ID of the queue to reconcile
     */
    suspend fun reconcile(queueId: UUID) {
        val queue: Queue = queues.getQueue(queueId) ?: return
        var updated: Queue?
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
        updated = when (internalState) {
            InternalState.NOT_ENOUGH_PLAYERS -> handleNotEnoughPlayers(queue)
            InternalState.SEARCHING_SERVER -> handleSearchingServer(queue)
            InternalState.WAITING_FOR_SERVER -> handleWaitingForServer(queue)
            InternalState.SERVER_READY -> handleServerReady(queue)
            InternalState.COUNTDOWN -> handleCountdown(queue)
            InternalState.TELEPORTING -> handleTeleporting(queue)
            InternalState.FINISHED -> handleFinished(queue)
        }
        if (updated == null) {
            queues.deleteQueue(queueId)
            clear(queueId)
            return
        }
        queues.updateQueue(updated)
        visualizer?.send(
            updated,
            types.find(updated.type)!!,
            queueInternalStates[updated.id] ?: InternalState.NOT_ENOUGH_PLAYERS
        )
    }

    /**
     * Updates the internal state of a queue.
     */
    private fun updateInternalState(queueId: UUID, newState: InternalState) {
        val oldState = queueInternalStates[queueId]
        queueInternalStates[queueId] = newState

        // If we're transitioning from COUNTDOWN to another state, clean up countdown data
        if (oldState == InternalState.COUNTDOWN && newState != InternalState.COUNTDOWN) {
            countdownStartTimes.remove(queueId)
            lastUpdateTimes.remove(queueId)
        }
    }

    /**
     * Handles a queue with NOT_ENOUGH_PLAYERS status.
     * Checks if there are enough players to start searching for a server.
     */
    private suspend fun handleNotEnoughPlayers(queue: Queue): Queue? {
        val type = types.find(queue.type) ?: return null
        if (queue.players.size >= type.minCapacity) {
            updateInternalState(queue.id, InternalState.SEARCHING_SERVER)
            reconcile(queue.id)
        }
        return queue
    }

    /**
     * Handles a queue with SEARCHING_SERVER status.
     * Attempts to find or request a server for the queue.
     */
    private suspend fun handleSearchingServer(queue: Queue): Queue? {
        val server = finder.reserveOrRequestServer(queue)
        if (server != null) {
            // Server found, update internal state
            if (server.state == ServerState.AVAILABLE) updateInternalState(queue.id, InternalState.SERVER_READY)
            queue.server = server
            reconcile(queue.id)
        }
        // No server found, a new one was requested, so we switch to the waiting state
        updateInternalState(queue.id, InternalState.WAITING_FOR_SERVER)
        return queue
    }

    /**
     * Handles a queue with WAITING_FOR_SERVER status.
     * Checks if a server has become available for the queue.
     */
    private suspend fun handleWaitingForServer(queue: Queue): Queue? {
        val server = finder.findServer(queue)
        if (server != null && server.state == ServerState.AVAILABLE) {
            updateInternalState(queue.id, InternalState.SERVER_READY)
            queue.server = server
            reconcile(queue.id)
        }
        return queue
    }

    /**
     * Handles a queue with SERVER_READY status.
     * Starts the countdown for teleporting players.
     */
    private fun handleServerReady(queue: Queue): Queue? {
        // Start countdown
        updateInternalState(queue.id, InternalState.COUNTDOWN)

        // Set initial countdown time
        val countdownDuration = 10.seconds.inWholeMilliseconds
        queue.countdownMillis = countdownDuration

        // Record start time for delta time calculation
        val currentTime = System.currentTimeMillis()
        countdownStartTimes[queue.id] = currentTime
        lastUpdateTimes[queue.id] = currentTime
        return queue
    }

    /**
     * Updates the countdown for a queue using delta time.
     * Returns the remaining time in milliseconds.
     */
    private fun updateCountdown(queue: Queue): Long {
        val currentTime = System.currentTimeMillis()
        val lastUpdateTime = lastUpdateTimes[queue.id] ?: return 0

        // Calculate delta time since last update
        val deltaTime = currentTime - lastUpdateTime
        lastUpdateTimes[queue.id] = currentTime

        // Calculate remaining time
        val remainingTime = (queue.countdownMillis ?: 0) - deltaTime
        queue.countdownMillis = maxOf(0, remainingTime)

        return queue.countdownMillis ?: 0
    }

    /**
     * Handles a queue with COUNTDOWN status.
     * Updates the countdown timer and transitions to TELEPORTING when done.
     */
    private fun handleCountdown(queue: Queue): Queue? {
        // The countdown is now handled by the updateCountdown method
        // This method is called during reconciliation, so we'll just update the countdown once
        val remaining = updateCountdown(queue)

        // If countdown is complete, move to teleporting
        if (remaining <= 0) {
            updateInternalState(queue.id, InternalState.TELEPORTING)
        }
        return queue
    }

    /**
     * Handles a queue with TELEPORTING status.
     * Teleports players to the server and marks the queue as finished.
     */
    private suspend fun handleTeleporting(queue: Queue): Queue? {
        val server = queue.server ?: return null
        val serverId = "${server.group}-${server.numericalId}"
        queue.players.forEach { playerId ->
            playerApi.getOnlinePlayer(playerId).sendMessage(Component.text("Starting game on server $serverId..."))
            playerApi.connectPlayer(playerId, serverId)
        }
        updateInternalState(queue.id, InternalState.FINISHED)
        reconcile(queue.id)
        return queue
    }

    /**
     * Handles a queue with FINISHED status.
     * Cleans up the queue.
     */
    private fun handleFinished(queue: Queue): Queue? {
        return null
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
            .filter { queueInternalStates[it.id] == InternalState.WAITING_FOR_SERVER }.forEach { queue ->
                if (finder.reserveServer(queue, server)) {
                    updateInternalState(queue.id, InternalState.SERVER_READY)
                    reconcile(queue.id)
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

    fun clear(id: UUID) {
        queueInternalStates.remove(id)
        countdownStartTimes.remove(id)
        lastUpdateTimes.remove(id)
    }

    fun startCountdownReconciliation() {
        scope.launch {
            while (true) {
                delay(500)
                queueInternalStates.filter { it.value == InternalState.COUNTDOWN }.forEach { (queueId) ->
                    val queue = queues.getQueue(queueId) ?: return@forEach
                    // Update countdown using delta time
                    val remaining = updateCountdown(queue)

                    // If countdown is complete, move to teleporting
                    if (remaining <= 0) {
                        updateInternalState(queue.id, InternalState.TELEPORTING)
                        queues.updateQueue(queue)
                    }
                    reconcile(queue.id)
                }
            }
        }
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
