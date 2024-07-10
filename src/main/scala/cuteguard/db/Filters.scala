package cuteguard.db

import cuteguard.model.discord.DiscordID

import cats.syntax.option.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

import java.util.UUID

object Filters:

  extension (id: UUID)
    def equalID: Filter     = fr"id = $id".some
    def equalUserID: Filter = fr"user_id = $id".some

  extension (id: Option[UUID]) def equalID: Filter = id.flatMap(_.equalID)

  extension (id: DiscordID) def equalDiscordID: Filter = fr"user_discord_id = $id".some
