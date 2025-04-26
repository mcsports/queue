package club.mcsports.droplet.queue.reconciler

enum class InternalState {
        NOT_ENOUGH_PLAYERS,
        SEARCHING_SERVER,
        WAITING_FOR_SERVER,
        SERVER_READY,
        COUNTDOWN,
        TELEPORTING,
        FINISHED
    }
