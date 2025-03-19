package club.mcsports.droplet.queue

import com.mcsports.queue.v1.*
import io.grpc.Status
import java.util.*

class QueueInteractionService(
        private val queues: QueueRepository
) : QueueInteractionGrpcKt.QueueInteractionCoroutineImplBase() {
    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val queue = queues.enqueue(request.queueName, request.playerIdsList.map { UUID.fromString(it) })
        if (queue == null)
            throw Status.INVALID_ARGUMENT.withDescription("Can not enqueue. Might be already in queue.").asRuntimeException()
        return enqueueResponse {
            queueId = queue.id.toString()
        }
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        if (!queues.dequeue(request.playerIdsList.map { UUID.fromString(it) }))
            throw Status.INVALID_ARGUMENT.withDescription("Can not dequeue. Might not be in queue.").asRuntimeException()
        return dequeueResponse { }
    }
}