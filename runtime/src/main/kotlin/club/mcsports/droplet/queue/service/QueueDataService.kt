package club.mcsports.droplet.queue.service

import club.mcsports.droplet.queue.QueueRepository
import club.mcsports.droplet.queue.QueueTypeRepository
import club.mcsports.droplet.queue.extension.log
import com.mcsports.queue.v1.*
import io.grpc.Status
import org.apache.logging.log4j.LogManager
import java.util.*

class QueueDataService(
    private val queueRepository: QueueRepository,
    private val typeRepository: QueueTypeRepository,
) : QueueDataGrpcKt.QueueDataCoroutineImplBase() {
    private val logger = LogManager.getLogger(QueueDataService::class.java)

    override suspend fun deleteQueue(request: DeleteQueueRequest): DeleteQueueResponse {
        if (!queueRepository.deleteQueue(UUID.fromString(request.queueId))) {
            throw Status.INTERNAL.withDescription("Failed to delete queue: No queue to identify ID ${request.queueId}")
                .log(logger).asRuntimeException()
        }
        return deleteQueueResponse {}
    }

    override suspend fun getAllQueues(request: GetAllQueuesRequest): GetAllQueuesResponse {
        return getAllQueuesResponse {
            queues.addAll(queueRepository.getAllQueues().map { it.toDefinition() })
        }
    }

    override suspend fun updateQueue(request: UpdateQueueRequest): UpdateQueueResponse {
        throw Status.UNIMPLEMENTED.log(logger).asRuntimeException()
    }

    override suspend fun getQueue(request: GetQueueRequest): GetQueueResponse {
        return getQueueResponse {
            queue = queueRepository.getQueue(UUID.fromString(request.queueId))?.toDefinition()
                ?: throw Status.NOT_FOUND.withDescription("Failed to get queue: Queue ${request.queueId} not found").log(logger).asRuntimeException()
        }
    }

    override suspend fun getAllQueueTypes(request: GetAllQueueTypesRequest): GetAllQueueTypesResponse {
        return getAllQueueTypesResponse {
            this.types.addAll(typeRepository.getAll().map { it.toDefinition() })
        }
    }

    override suspend fun getQueueByPlayer(request: GetQueueByPlayerRequest): GetQueueByPlayerResponse {
        val queue = queueRepository.getQueueByPlayer(UUID.fromString(request.playerId))
            ?: throw Status.NOT_FOUND.withDescription("Failed to get queue: Player ${request.playerId} is not in any queue.")
                .log(logger).asRuntimeException()
        return getQueueByPlayerResponse {
            this.result = queue.toDefinition()
        }
    }

    override suspend fun getQueueTypePlayerInformation(request: GetQueueTypePlayerInformationRequest): GetQueueTypePlayerInformationResponse {
        val type = typeRepository.find(request.type)
            ?: throw Status.NOT_FOUND.withDescription("Failed to get queue: Type ${request.type} not found").log(logger).asRuntimeException()
        val queues = queueRepository.getAllQueuesByType(type.name)
        return getQueueTypePlayerInformationResponse {
            this.queueing = queues.sumOf { it.players.size }
            this.playing = 0 //TODO: implement this
            this.playingPrivately = 0 //TODO: implement this
            this.totalPopularity = this.queueing + this.playing + this.playingPrivately
        }
    }
}