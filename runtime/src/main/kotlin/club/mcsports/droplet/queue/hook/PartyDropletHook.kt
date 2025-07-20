package club.mcsports.droplet.queue.hook

import club.mcsports.droplet.party.api.PartyApi
import org.apache.logging.log4j.LogManager

object PartyDropletHook {
    private val logger = LogManager.getLogger(PartyDropletHook::class.java)

    val api = try {
        PartyApi.createCoroutineApi(
            authSecret,
            System.getenv("PARTY_HOST") ?: "0.0.0.0",
            System.getenv("PARTY_PORT")?.toInt() ?: 5831
        )
    } catch (exception: Exception) {
        if(exception is ClassNotFoundException) {
            logger.warn("Failed to load party api: Couldn't find api classes. No party features will be considered")
        } else logger.error(exception.stackTraceToString())
        null
    }

}