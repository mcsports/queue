package club.mcsports.droplet.queue.plugin

import app.simplecloud.plugin.api.shared.extension.text
import com.velocitypowered.api.proxy.Player

object CommandHelp {

    private val help = mutableMapOf(
        "queue <type>" to "Enqueues you for the given type",
        "dequeue" to "Takes you out of the queue you are in",
    )

    fun Player.sendHelp() {
        this.sendMessage(text("Commands of Queue"))
        help.forEach { (command, description) ->
            this.sendMessage(text("<gray>/$command"))
        }

    }
}