package club.mcsports.droplet.queue.service

import club.mcsports.droplet.queue.QueueRepository
import com.mcsports.queue.v1.*
import io.grpc.Status
import java.util.*

class QueueDataService(
    private val queueRepository: QueueRepository,
) : QueueDataGrpcKt.QueueDataCoroutineImplBase() {
    override suspend fun deleteQueue(request: DeleteQueueRequest): DeleteQueueResponse {
        if (!queueRepository.deleteQueue(UUID.fromString(request.queueId))) {
            throw Status.INTERNAL.withDescription("Failed to delete queue ${request.queueId}").asRuntimeException()
        }
        return deleteQueueResponse {}
    }

    override suspend fun getAllQueues(request: GetAllQueuesRequest): GetAllQueuesResponse {
        return getAllQueuesResponse {
            queues.addAll(queueRepository.getAllQueues().map { it.toDefinition() })
        }
    }

    override suspend fun updateQueue(request: UpdateQueueRequest): UpdateQueueResponse {
        throw Status.UNIMPLEMENTED.asRuntimeException()
    }

    override suspend fun getQueue(request: GetQueueRequest): GetQueueResponse {
        return getQueueResponse {
            queue = queueRepository.getQueue(UUID.fromString(request.queueId))?.toDefinition()
                ?: throw Status.NOT_FOUND.withDescription("Queue ${request.queueId} not found").asRuntimeException()
        }
    }
}