package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.{CuteGuardUser, Discord, DiscordID}
import cuteguard.utils.Maybe

import cats.effect.{Deferred, IO}
import cats.effect.LiftIO.*
import doobie.{Fragment, LogHandler, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.connectionio.*
import doobie.syntax.string.*

import java.time.Instant
import java.util.UUID

case class User(
  id: UUID,
  discordID: DiscordID,
  guildID: DiscordID,
  points: Int,
  pointsToday: Int,
  pointsLastUpdate: Instant,
)

class CuteGuardUserRepository(
  discord: Deferred[IO, Discord],
)(using
  transactor: Transactor[IO],
  logHandler: LogHandler,
) extends ModelRepository[User, CuteGuardUser] with ThoroughList[User, CuteGuardUser, String]:
  override protected val table: Fragment       = fr"users"
  override protected val columns: List[String] = List(
    "id",
    "user_discord_id",
    "guild_discord_id",
    "points",
    "points_today",
    "last_point_update",
  )

  override def id(db: User): String =
    s"guild ID: ${db.guildID}, discord: <@!${db.discordID}>(${db.discordID}) points: ${db.points}"

  override def toModel(user: User): Maybe[CuteGuardUser] =
    for
      discord <- discord.get.to[Maybe]
      guild   <- discord.guildByID(user.guildID)
      member  <- guild.member(user.discordID)
    yield new CuteGuardUser(
      user.id,
      member,
      user.points,
      user.pointsToday,
      user.pointsLastUpdate,
    )

  def add(
    discordID: DiscordID,
    guildID: DiscordID,
    points: Int,
  ): IO[CuteGuardUser] =
    sql"insert into $table (user_discord_id, guild_discord_id, points, points_today, last_point_update) values ($discordID, $guildID, $points, $points, ${Instant
        .now()})".update
      .withUniqueGeneratedKeys[User](columns*)
      .transact(transactor)
      .flatMap(unsafeToModel)

  def update(
    id: UUID,
    points: Option[Int] = None,
    pointsToday: Option[Int] = None,
    discordID: Option[DiscordID] = None,
  ): IO[CuteGuardUser] =
    update(
      points.map(points => fr"points = $points"),
      pointsToday.map(pointsToday => fr"points_today = $pointsToday"),
      discordID.map(keyholderIDs => fr"user_discord_id = $keyholderIDs"),
      Some(fr"last_point_update = ${Instant.now()}"),
    )(fr"id = $id")
