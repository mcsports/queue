package club.mcsports.droplet.queue.api.impl.future

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import club.mcsports.droplet.queue.api.DataApi
import club.mcsports.droplet.queue.api.InteractionApi
import club.mcsports.droplet.queue.api.QueueApi
import io.grpc.ManagedChannelBuilder

class QueueApiFutureImpl(
    authSecret: String,
    host: String,
    port: Int,
) : QueueApi.Future {

    private val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    private val credentials = AuthCallCredentials(authSecret)
    private val dataApi = QueueDataApiFutureImpl(channel, credentials)
    private val interactionApi = QueueInteractionApiFutureImpl(channel, credentials, dataApi)

    override fun getData(): DataApi.Future {
        return dataApi
    }

    override fun getInteraction(): InteractionApi.Future {
        return interactionApi
    }
}