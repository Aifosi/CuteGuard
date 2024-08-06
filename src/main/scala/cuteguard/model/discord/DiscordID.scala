package cuteguard.model.discord

import doobie.{Get, Put}
import pureconfig.ConfigReader

opaque type DiscordID = Long

object DiscordID:
  given Get[DiscordID] = Get[Long].tmap(DiscordID(_))
  given Put[DiscordID] = Put[Long]

  given ConfigReader[DiscordID] = ConfigReader.longConfigReader

  def apply(id: Long): DiscordID = id

extension (id: DiscordID) def toLong: Long = id
