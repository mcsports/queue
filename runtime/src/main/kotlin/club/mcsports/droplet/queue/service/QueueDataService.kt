package club.mcsports.droplet.queue.service

import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.QueueTypeRepository
import com.mcsports.queue.v1.*
import io.grpc.Status
import java.util.*

class QueueDataService(
    private val queueRepository: QueueRepository,
    private val typeRepository: QueueTypeRepository,
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

    override suspend fun getAllQueueTypes(request: GetAllQueueTypesRequest): GetAllQueueTypesResponse {
        return getAllQueueTypesResponse {
            this.types.addAll(typeRepository.getAll().map { it.toDefinition() })
        }
    }

    override suspend fun getQueueByPlayer(request: GetQueueByPlayerRequest): GetQueueByPlayerResponse {
        val queue = queueRepository.getQueueByPlayer(UUID.fromString(request.playerId))
            ?: throw Status.NOT_FOUND.withDescription("Player ${request.playerId} is not in any queue.")
                .asRuntimeException()
        return getQueueByPlayerResponse {
            this.result = queue.toDefinition()
        }
    }

    override suspend fun getQueueTypePlayerInformation(request: GetQueueTypePlayerInformationRequest): GetQueueTypePlayerInformationResponse {
        val type = typeRepository.find(request.type)
            ?: throw Status.NOT_FOUND.withDescription("Type ${request.type} not found").asRuntimeException()
        val queues = queueRepository.getAllQueuesByType(type.name)
        return getQueueTypePlayerInformationResponse {
            this.queueing = queues.sumOf { it.players.size }
            this.playing = 0 //TODO: implement this
            this.playingPrivately = 0 //TODO: implement this
            this.totalPopularity = this.queueing + this.playing + this.playingPrivately
        }
    }
}