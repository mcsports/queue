package club.mcsports.droplet.queue.visualizer

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.argument.PropertiesArgumentsResolver
import app.simplecloud.plugin.api.shared.pretty.StringPrettifier
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object ServerTagResolver {

    fun get(server: Server): TagResolver {
        val propertiesResolver = PropertiesArgumentsResolver(server.properties)
        return TagResolver.resolver(
            TagResolver.resolver("server_id", Tag.selfClosingInserting(Component.text(server.uniqueId))),
            TagResolver.resolver("server_numerical_id", Tag.selfClosingInserting(Component.text(server.numericalId))),
            TagResolver.resolver("server_group_name", Tag.selfClosingInserting(Component.text(server.group))),
            TagResolver.resolver(
                "server_group_pretty_name",
                Tag.selfClosingInserting(
                    Component.text(
                        server.properties["pretty-name"] ?: StringPrettifier.prettify(server.group)
                    )
                )
            ),
            TagResolver.resolver("server_type", Tag.selfClosingInserting(Component.text(server.type.toString()))),
            TagResolver.resolver("server_state", Tag.selfClosingInserting(Component.text(server.state.toString()))),
            TagResolver.resolver("server_ip", Tag.selfClosingInserting(Component.text(server.ip))),
            TagResolver.resolver("server_port", Tag.selfClosingInserting(Component.text(server.port))),
            TagResolver.resolver("server_online_players", Tag.selfClosingInserting(Component.text(server.playerCount))),
            TagResolver.resolver("server_max_players", Tag.selfClosingInserting(Component.text(server.maxPlayers))),
            TagResolver.resolver("server_min_memory", Tag.selfClosingInserting(Component.text(server.minMemory))),
            TagResolver.resolver("server_max_memory", Tag.selfClosingInserting(Component.text(server.maxMemory))),
            TagResolver.resolver(
                "server_motd",
                Tag.selfClosingInserting(Component.text(server.properties["motd"] ?: "A Minecraft Server"))
            ),
            TagResolver.resolver("server_${propertiesResolver.getKey()}") { arguments, _ ->
                return@resolver runBlocking { propertiesResolver.resolve(arguments) }
            }
        )
    }
}