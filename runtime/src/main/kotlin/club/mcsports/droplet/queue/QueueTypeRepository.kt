package club.mcsports.droplet.queue

import app.simplecloud.plugin.api.shared.config.repository.DirectoryRepository
import app.simplecloud.plugin.api.shared.config.repository.handler.YamlFileHandler
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path

typealias QueueTypeRepository = DirectoryRepository<String, QueueType>

object QueueTypeRepositoryInitializer {
    fun create(directory: Path): QueueTypeRepository {
        return DirectoryRepository(directory, YamlFileHandler(QueueType::class.java), Dispatchers.IO)
    }
}