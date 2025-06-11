package club.mcsports.droplet.queue.hook

import club.mcsports.droplet.party.api.PartyApi
import org.apache.logging.log4j.LogManager

class PartyDropletHook {

    val api = try {
        PartyApi.createCoroutineApi()
    } catch (_: ClassNotFoundException) {
        LogManager.getLogger(PartyDropletHook::class.java)
            .warn("Failed to load party api: Couldn't find api classes. No party features will be considered")
    }

}