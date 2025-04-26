package club.mcsports.droplet.queue.api.impl.future

import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.future.toCompletable
import club.mcsports.droplet.queue.Queue
import club.mcsports.droplet.queue.api.DataApi
import com.mcsports.queue.v1.*
import io.grpc.ManagedChannel
import java.util.concurrent.CompletableFuture

class QueueDataApiFutureImpl(
    channel: ManagedChannel,
    credentials: AuthCallCredentials,
) : DataApi.Future {

    private val api = QueueDataGrpc.newFutureStub(channel).withCallCredentials(credentials)

    override fun getAllQueues(): CompletableFuture<List<Queue>> {
        return api.getAllQueues(GetAllQueuesRequest.newBuilder().build()).toCompletable().thenApply { res ->
            res.queuesList.map { Queue.fromDefinition(it) }
        }
    }

    override fun getQueue(id: String): CompletableFuture<Queue?> {
        return api.getQueue(GetQueueRequest.newBuilder().setQueueId(id).build()).toCompletable().thenApply { res ->
            res.queue?.let { Queue.fromDefinition(it) }
        }
    }

    override fun updateQueue(queue: Queue): CompletableFuture<Queue?> {
        return api.updateQueue(
            UpdateQueueRequest.newBuilder().setQueueId(queue.id.toString()).setUpdated(queue.toDefinition()).build()
        ).toCompletable().thenApply { res ->
            res.result?.let { Queue.fromDefinition(it) }
        }
    }

    override fun deleteQueue(id: String): CompletableFuture<Boolean> {
        return api.deleteQueue(DeleteQueueRequest.newBuilder().setQueueId(id).build()).toCompletable().thenApply {
            true
        }.exceptionally { false }
    }
}