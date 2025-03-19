package club.mcsports.droplet.queue

import com.mcsports.queue.v1.*

class QueueInteractionService : QueueInteractionGrpcKt.QueueInteractionCoroutineImplBase() {
    override suspend fun enqueue(request: EnqueueRequest): EnqueueResponse {
        return super.enqueue(request)
    }

    override suspend fun dequeue(request: DequeueRequest): DequeueResponse {
        return super.dequeue(request)
    }
}