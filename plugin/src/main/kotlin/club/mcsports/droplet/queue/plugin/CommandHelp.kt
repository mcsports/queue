package club.mcsports.droplet.queue.plugin


import club.mcsports.droplet.queue.Glyphs
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object CommandHelp {

    private val help = mutableMapOf(
        "queue <type>" to "Enqueues you for the given type",
        "dequeue" to "Takes you out of the queue you are in",
    )

    fun Player.sendHelp() {
        this.sendMessage(
            Glyphs.HOUR_GLASS.append(Component.text(" Commands of Queue").color(NamedTextColor.WHITE).append(
                help.keys.fold(Component.empty()) { accumulator, command ->
                    val commandComponent = Component.text("   /$command").color(NamedTextColor.GRAY)
                    accumulator.append(Component.newline()).append(commandComponent)
                }
            )
        ))
    }
}