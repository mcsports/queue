package club.mcsports.droplet.queue.plugin

import club.mcsports.droplet.queue.api.QueueApi
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class QueueCommand(
    private val queueApi: QueueApi.Coroutine
) : SimpleCommand {
    override fun execute(invocation: SimpleCommand.Invocation) {
        if (invocation.arguments().size != 1) return
        val type = invocation.arguments()[0]
        val player = invocation.source() as? Player ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val queue = queueApi.getInteraction().enqueue(type, player.uniqueId)
            if (queue == null) {
                player.sendMessage(Component.text("Failed to enqueue").color(NamedTextColor.RED))
                return@launch
            }
            player.sendMessage(Component.text("Enqueued $type (${queue.id})").color(NamedTextColor.GREEN))
        }
    }
}