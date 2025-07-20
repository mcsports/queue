package club.mcsports.droplet.queue.plugin


import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.Glyphs
import club.mcsports.droplet.queue.api.QueueApi
import club.mcsports.droplet.queue.plugin.CommandHelp.sendHelp
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component

class DequeueCommand(
    private val queueApi: QueueApi.Coroutine,
) : SimpleCommand {
    override fun execute(invocation: SimpleCommand.Invocation) {
        val player = invocation.source() as? Player ?: run {
            invocation.source().sendMessage(Glyphs.HOUR_GLASS.append(Component.text(" You have to be a player to do this.").color(Color.RED)))
            return
        }

        if (invocation.arguments().isNotEmpty()) {
            player.sendHelp()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            queueApi.getInteraction().dequeue(player.uniqueId)
        }
    }
}