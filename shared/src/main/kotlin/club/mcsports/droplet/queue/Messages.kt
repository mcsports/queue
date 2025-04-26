package club.mcsports.droplet.queue

import club.mcsports.droplet.queue.reconciler.InternalState

typealias Messages = Map<InternalState, String>

val defaultMessages: Messages = mapOf(
    InternalState.NOT_ENOUGH_PLAYERS to "Waiting for players (<queue_players>/<queue_max_capacity>)",
    InternalState.SEARCHING_SERVER to "Searching for a <queue_type> server (<queue_players>/<queue_max_capacity>)",
    InternalState.WAITING_FOR_SERVER to "Waiting for a <queue_type> server (<queue_players>/<queue_max_capacity>)",
    InternalState.SERVER_READY to "Server found! (<queue_players>/<queue_max_capacity>)",
    InternalState.COUNTDOWN to "Game starting in <queue_countdown_seconds> seconds (<queue_players>/<queue_max_capacity>)",
    InternalState.TELEPORTING to "Teleporting to <server_group_name>-<server_numerical_id>...",
)