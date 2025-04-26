package club.mcsports.droplet.queue.plugin

import club.mcsports.droplet.queue.api.QueueApi
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class DequeueCommand(
    private val queueApi: QueueApi.Coroutine
) : SimpleCommand {
    override fun execute(invocation: SimpleCommand.Invocation) {
        val player = invocation.source() as? Player ?: return
        CoroutineScope(Dispatchers.IO).launch {
            if (!queueApi.getInteraction().dequeue(player.uniqueId)) {
                player.sendMessage(Component.text("Failed to dequeue").color(NamedTextColor.RED))
                return@launch
            }
            player.sendMessage(Component.text("Successfully dequeued.").color(NamedTextColor.GREEN))
        }
    }
}