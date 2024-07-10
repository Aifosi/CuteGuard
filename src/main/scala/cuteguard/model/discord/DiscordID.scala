package cuteguard.model.discord

import doobie.{Get, Put}
import doobie.util.{Read, Write}
import pureconfig.ConfigReader

opaque type DiscordID = Long

object DiscordID:
  given Read[DiscordID]  = Read.fromGet[Long]
  given Write[DiscordID] = Write.fromPut[Long]

  given Get[DiscordID] = Get[Long].map(DiscordID(_))
  given Put[DiscordID] = Put[Long]

  given ConfigReader[DiscordID] = ConfigReader.longConfigReader

  def apply(id: Long): DiscordID = id

extension (id: DiscordID) def toLong: Long = id

given Conversion[Long, DiscordID] = DiscordID.apply
