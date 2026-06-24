package net.aitorciki.dem3ux.ui.nav

import kotlinx.serialization.Serializable

sealed interface MainRoute

@Serializable
object PlaylistsRoute : MainRoute

@Serializable
object SetupRoute : MainRoute

@Serializable
object HelpRoute : MainRoute
