package cuteguard

import cuteguard.model.discord.DiscordID

import pureconfig.ConfigReader

case class DiscordConfiguration(
  token: String,
  logChannelID: Option[DiscordID],
) derives ConfigReader

object DiscordConfiguration:
  given ConfigReader[(Int, DiscordID)] = ConfigReader.derived[(Int, DiscordID)]
