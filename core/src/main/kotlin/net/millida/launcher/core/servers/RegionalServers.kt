package net.millida.launcher.core.servers

import net.millida.launcher.core.rating.RatedServer

object RegionalServers {
    val suggestions: List<RatedServer> = listOf(
        RatedServer(
            name = "BDZONE",
            slug = "suggested-bdzone",
            ip = "play.bdzonemc.com",
            address = "play.bdzonemc.com",
            shortDesc = "Bangladesh · Java and Bedrock",
            bedrockAddress = "pe.bdzonemc.com:19132",
            suggested = true,
        ),
        RatedServer(
            name = "Cyan Plex",
            slug = "suggested-cyanplex",
            ip = "play.cyanplex.net",
            address = "play.cyanplex.net",
            shortDesc = "Bangladesh · Java and Bedrock",
            bedrockAddress = "play.cyanplex.net:19132",
            suggested = true,
        ),
        RatedServer(
            name = "Royal SMP",
            slug = "suggested-royal-smp",
            ip = "play.royallsmp.net",
            address = "play.royallsmp.net",
            shortDesc = "Bangladesh · Java",
            suggested = true,
        ),
        RatedServer(
            name = "Foxcraft",
            slug = "suggested-foxcraft",
            ip = "mp.foxcraft.net",
            address = "mp.foxcraft.net",
            shortDesc = "United States · Java and Bedrock",
            suggested = true,
        ),
        RatedServer(
            name = "SmileMoreCraft",
            slug = "suggested-smilemorecraft",
            ip = "mp.smilemorecraft.com",
            address = "mp.smilemorecraft.com",
            shortDesc = "United States · Java and Bedrock",
            bedrockAddress = "play.smilemorecraft.com:19132",
            suggested = true,
        ),
    )
}
