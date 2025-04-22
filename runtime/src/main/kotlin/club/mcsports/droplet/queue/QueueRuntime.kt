package club.mcsports.droplet.queue

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.droplet.api.auth.AuthSecretInterceptor
import app.simplecloud.droplet.player.api.PlayerApi
import club.mcsports.droplet.queue.launcher.QueueStartCommand
import club.mcsports.droplet.queue.reconciler.QueueStatusReconciler
import club.mcsports.droplet.queue.server.ServerFinder
import club.mcsports.droplet.queue.service.QueueDataService
import club.mcsports.droplet.queue.service.QueueInteractionService
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.logging.log4j.LogManager

class QueueRuntime(
    private val args: QueueStartCommand
) {

    companion object {
        private val logger = LogManager.getLogger(QueueRuntime::class.java)
    }

    private val controllerApi = ControllerApi.createCoroutineApi(args.authSecret)
    private val playerApi = PlayerApi.createCoroutineApi(args.authSecret)
    private val pubSubClient = controllerApi.getPubSubClient()

    private val typeRepository = QueueTypeRepository()
    private val queueRepository = QueueRepository(typeRepository)
    private val finder = ServerFinder(controllerApi, typeRepository)
    val reconciler = QueueStatusReconciler(queueRepository, typeRepository, finder, playerApi)

    private val server = createGrpcServer()

    suspend fun start() {
        queueRepository.setReconciler(reconciler)
        logger.info("Starting queue reconciler...")
        reconciler.startPeriodicReconciliation()
        reconciler.registerServerRegistrationSubscriber(pubSubClient)
        startGrpcServer()

        suspendCancellableCoroutine { continuation ->
            Runtime.getRuntime().addShutdownHook(Thread {
                server.shutdown()
                continuation.resume(Unit) { cause, _, _ ->
                    logger.info("Server shutdown due to: $cause")
                }
            })
        }
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.start()
                server.awaitTermination()
            } catch (e: Exception) {
                logger.error("Error in gRPC server", e)
                throw e
            }
        }
    }

    private fun createGrpcServer(): Server {
        return ServerBuilder.forPort(args.grpcPort)
            .addService(QueueInteractionService(queueRepository))
            .addService(QueueDataService(queueRepository))
            .intercept(AuthSecretInterceptor(args.grpcHost, args.authorizationPort))
            .build()
    }
}
