package club.mcsports.droplet.queue.plugin

import club.mcsports.droplet.queue.api.QueueApi
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.Logger

@Plugin(
    id = "mcsports-queue", name = "Queue", version = "1.0.0", authors = ["ugede"], dependencies = [
        Dependency(id = "simplecloud-api")
    ]
)
class QueueVelocityPlugin() {

    lateinit var server: ProxyServer

    lateinit var logger: Logger

    lateinit var api: QueueApi.Coroutine

    @Inject
    constructor(server: ProxyServer, logger: Logger) : this() {
        this.server = server
        this.logger = logger
    }

    @Subscribe
    fun onInit(event: ProxyInitializeEvent) {
        api = QueueApi.createCoroutineApi()
        server.commandManager.register(
            server.commandManager.metaBuilder("queue").plugin(this).build(),
            QueueCommand(api)
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("dequeue").plugin(this).build(),
            DequeueCommand(api)
        )
        logger.info("Initializing mcsports-queue")
    }

    @Subscribe
    fun onQuit(event: DisconnectEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            api.getInteraction().dequeue(event.player.uniqueId, force = true)
        }
    }
}