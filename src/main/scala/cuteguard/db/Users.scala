package cuteguard.db

import cuteguard.model.User as CuteguardUser
import cuteguard.model.discord.{DiscordID, Guild}
import cuteguard.utils.Maybe

import cats.effect.IO
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.string.*

import java.util.UUID

case class User(
  id: UUID,
  discordID: DiscordID,
)

class Users(guild: Guild)(using Transactor[IO]) extends ModelRepository[User, CuteguardUser]:
  override protected val table: Fragment = fr"users"

  override protected val columns: List[String]           = List(
    "id",
    "user_discord_id",
  )
  override def toModel(user: User): Maybe[CuteguardUser] = guild.member(user.discordID).map { member =>
    CuteguardUser(
      user.id,
      member,
    )
  }
