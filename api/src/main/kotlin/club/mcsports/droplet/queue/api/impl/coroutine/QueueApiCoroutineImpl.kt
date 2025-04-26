package club.mcsports.droplet.queue.api.impl.coroutine

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import club.mcsports.droplet.queue.api.DataApi
import club.mcsports.droplet.queue.api.InteractionApi
import club.mcsports.droplet.queue.api.QueueApi
import io.grpc.ManagedChannelBuilder

class QueueApiCoroutineImpl(
    authSecret: String,
    host: String,
    port: Int,
) : QueueApi.Coroutine {

    private val channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build()
    private val credentials = AuthCallCredentials(authSecret)
    private val dataApi = QueueDataApiCoroutineImpl(credentials, channel)
    private val interactionApi = QueueInteractionApiCoroutineImpl(credentials, channel, dataApi)

    override fun getData(): DataApi.Coroutine {
        return dataApi
    }

    override fun getInteraction(): InteractionApi.Coroutine {
        return interactionApi
    }
}