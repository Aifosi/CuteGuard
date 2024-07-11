package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.User as CuteguardUser
import cuteguard.model.discord.{DiscordID, Guild, User as DiscordUser}
import cuteguard.utils.Maybe

import cats.data
import cats.data.OptionT
import cats.effect.{IO, Ref}
import cats.syntax.applicative.*
import cats.syntax.option.*
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.string.*

import java.util.UUID

case class User(
  id: UUID,
  discordID: DiscordID,
)

class Users(
  guild: Maybe[Guild],
  uuidCache: Ref[IO, Map[UUID, CuteguardUser]],
  discordIDCache: Ref[IO, Map[DiscordID, CuteguardUser]],
)(using Transactor[IO])
    extends ModelRepository[User, CuteguardUser]:
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

  private def updateCache(user: CuteguardUser): IO[Unit] =
    for
      _ <- uuidCache.update(_ + (user.id -> user))
      _ <- discordIDCache.update(_ + (user.discordID -> user))
    yield ()

  def add(user: DiscordUser): IO[CuteguardUser] =
    findByDiscordID(user.discordID)
      .getOrElseF(insertOne(user.discordID)(columns*))
      .flatTap(updateCache)

  def findByID(id: UUID): OptionT[IO, CuteguardUser] =
    OptionT {
      OptionT(uuidCache.get.map(_.get(id)))
        .foldF(find(id.equalID).semiflatTap(updateCache).value)(_.some.pure)
    }

  def findByDiscordID(discordID: DiscordID): OptionT[IO, CuteguardUser] =
    OptionT {
      OptionT(discordIDCache.get.map(_.get(discordID)))
        .foldF(find(discordID.equalDiscordID).semiflatTap(updateCache).value)(_.some.pure)
    }

  def getByID(id: UUID): IO[CuteguardUser] =
    uuidCache.get.map(_.get(id)).flatMap {
      case Some(user) => IO.pure(user)
      case None       => get(id.equalID).flatTap(updateCache)
    }

  def getByDiscordID(discordID: DiscordID): IO[CuteguardUser] =
    discordIDCache.get.map(_.get(discordID)).flatMap {
      case Some(user) => IO.pure(user)
      case None       => get(discordID.equalDiscordID).flatTap(updateCache)
    }

object Users:
  def apply(guild: Maybe[Guild])(using Transactor[IO]): IO[Users] =
    for
      uuidCache      <- Ref.of[IO, Map[UUID, CuteguardUser]](Map.empty)
      discordIDCache <- Ref.of[IO, Map[DiscordID, CuteguardUser]](Map.empty)
    yield new Users(guild, uuidCache, discordIDCache)
