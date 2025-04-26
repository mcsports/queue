package club.mcsports.droplet.queue.api

import club.mcsports.droplet.queue.api.impl.coroutine.QueueApiCoroutineImpl
import club.mcsports.droplet.queue.api.impl.future.QueueApiFutureImpl

interface QueueApi {

    interface Coroutine {
        fun getData(): DataApi.Coroutine
        fun getInteraction(): InteractionApi.Coroutine
    }

    interface Future {
        fun getData(): DataApi.Future
        fun getInteraction(): InteractionApi.Future
    }


    companion object {
        @JvmStatic
        fun createFutureApi(authSecret: String): Future {
            return createFutureApi(
                System.getenv("CONTROLLER_SECRET"),
                System.getenv("QUEUE_HOST") ?: "0.0.0.0",
                System.getenv("QUEUE_PORT")?.toInt() ?: 5830
            )
        }

        @JvmStatic
        fun createFutureApi(authSecret: String, host: String, port: Int): Future {
            return QueueApiFutureImpl(authSecret, host, port)
        }

        @JvmStatic
        fun createCoroutineApi(): Coroutine {
            return createCoroutineApi(
                System.getenv("CONTROLLER_SECRET"),
                System.getenv("QUEUE_HOST") ?: "0.0.0.0",
                System.getenv("QUEUE_PORT")?.toInt() ?: 5830,
            )
        }

        @JvmStatic
        fun createCoroutineApi(authSecret: String, host: String, port: Int): Coroutine {
            return QueueApiCoroutineImpl(authSecret, host, port)
        }
    }
}