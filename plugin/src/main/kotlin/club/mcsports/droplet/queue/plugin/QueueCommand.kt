package club.mcsports.droplet.queue.plugin

import app.simplecloud.plugin.api.shared.extension.text
import club.mcsports.droplet.queue.Color
import club.mcsports.droplet.queue.api.QueueApi
import club.mcsports.droplet.queue.plugin.CommandHelp.sendHelp
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class QueueCommand(
    private val queueApi: QueueApi.Coroutine
) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val player = invocation.source() as? Player ?: run {
            invocation.source().sendMessage(text("${Color.RED}You have to be a player to do this."))
            return
        }

        if (invocation.arguments().size != 1) {
            player.sendHelp()
            return
        }

        val type = invocation.arguments()[0]

        CoroutineScope(Dispatchers.IO).launch {
            queueApi.getInteraction().enqueue(type, player.uniqueId)
        }
    }

    override fun suggestAsync(invocation: SimpleCommand.Invocation): CompletableFuture<MutableList<String>> {
        return CoroutineScope(Dispatchers.IO).async {
            queueApi.getData().getAllQueueTypes().map { it.name }.toMutableList()
        }.asCompletableFuture()
    }
}