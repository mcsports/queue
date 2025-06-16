package club.mcsports.droplet.queue.extension

import app.simplecloud.droplet.player.api.CloudPlayer
import club.mcsports.droplet.queue.QueueRuntime
import java.util.UUID

fun String.asUuid() = UUID.fromString(this)

private val playerApi = QueueRuntime.playerApiSingleton

suspend fun String.fetchPlayer(): CloudPlayer {
    return try {
        playerApi.getOnlinePlayer(UUID.fromString(this))
    } catch(_: IllegalArgumentException) {
        playerApi.getOnlinePlayer(this)
    }
}

suspend fun UUID.fetchPlayer(): CloudPlayer = playerApi.getOnlinePlayer(this)