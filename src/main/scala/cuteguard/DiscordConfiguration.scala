package cuteguard

import cuteguard.model.discord.DiscordID

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.derived

case class DiscordConfiguration(
  token: String,
  logChannelID: Option[DiscordID],
) derives ConfigReader

object DiscordConfiguration:
  given ConfigReader[(Int, DiscordID)] = ConfigReader.derived[(Int, DiscordID)]
