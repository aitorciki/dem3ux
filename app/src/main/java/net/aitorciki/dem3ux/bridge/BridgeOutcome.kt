package net.aitorciki.dem3ux.bridge

sealed interface BridgeOutcome {
    data object Launched : BridgeOutcome

    data class NeedsFolderAccess(
        val bridgeLaunch: BridgeLaunch,
    ) : BridgeOutcome

    data class Failed(
        val message: String,
        val error: Throwable? = null,
    ) : BridgeOutcome
}
