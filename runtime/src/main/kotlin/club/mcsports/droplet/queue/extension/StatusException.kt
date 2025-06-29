package club.mcsports.droplet.queue.extension

import io.grpc.Status
import org.apache.logging.log4j.Logger

fun Status.log(logger: Logger): Status {
    logger.warn("Status ${this.code}: ${this.description}")
    return this
}