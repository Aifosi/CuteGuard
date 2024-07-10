package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.User as CuteguardUser
import cuteguard.model.discord.{DiscordID, Guild, User as DiscordUser}
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

class Users(guild: Maybe[Guild])(using Transactor[IO]) extends ModelRepository[User, CuteguardUser]:
  override protected val table: Fragment = fr"users"

  override protected val columns: List[String]           = List(
    "user_discord_id",
  )
  override def toModel(user: User): Maybe[CuteguardUser] =
    for
      guild  <- guild
      member <- guild.member(user.discordID)
    yield CuteguardUser(
      user.id,
      member,
    )

  def add(user: DiscordUser): IO[CuteguardUser] =
    find(user.discordID.equalDiscordID)
      .getOrElseF(insertOne(user.discordID)(columns*))
