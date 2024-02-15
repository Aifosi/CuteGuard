package cuteguard

import cuteguard.Points.{addRole, rankForPoints, removeRoles}
import cuteguard.db.CuteGuardUserRepository
import cuteguard.db.Filters.equalDiscordAndGuildID
import cuteguard.model.{Channel, CuteGuardUser, Discord, Member, Role}

import cats.effect.{Deferred, IO}
import cats.syntax.foldable.*
import cats.syntax.option.*
import cats.syntax.parallel.*
import cats.syntax.traverse.*
import org.typelevel.log4cats.Logger

import java.time.{Instant, ZoneId}

class Points(
  discord: Deferred[IO, Discord],
  cuteGuardUserRepository: CuteGuardUserRepository,
  config: CuteguardConfiguration,
)(using
  logger: Logger[IO],
  discordLogger: DiscordLogger,
):
  lazy val ranks: IO[List[(Int, Role)]] = discord.get.flatMap { discord =>
    config.ranks.traverse((points, id) => discord.unsafeRoleByID(id).map(points -> _))
  }

  private def createNewUser(member: Member): IO[CuteGuardUser] =
    for
      _    <- discordLogger.logToChannel(s"Found new user ${member.effectiveName}.")
      user <- cuteGuardUserRepository.add(member.discordID, member.guild.discordID, 0)
    yield user

  private def handleRanks(user: CuteGuardUser, totalPoints: Int, channel: Channel): IO[Unit] =
    for
      ranks        <- ranks
      currentRoles <-
        ranks.parUnorderedFlatTraverse((_, role) => user.hasRole(user.guild, role).map(Option.when(_)(role).toList))
      _             = ranks.rankForPoints(totalPoints).fold(user.removeRoles(currentRoles)) { rankForPoints =>
                        for
                          _ <- IO.unlessA(currentRoles.contains(rankForPoints))(user.addRole(channel, rankForPoints))
                          _ <- user.removeRoles(currentRoles.filter(_ != rankForPoints))
                        yield ()
                      }
    yield ()

  private def addPoints(user: CuteGuardUser, channel: Channel, maxPointsToAdd: Int): IO[Unit] =
    val pointsAddedToday = if Points.isToday(user.pointsLastUpdate) then user.pointsToday else 0
    val remainingPoints  = config.maxDailyPoints - pointsAddedToday
    if remainingPoints == 0 then IO.unit
    else
      for
        pointsToAdd <- IO.pure(math.min(remainingPoints, maxPointsToAdd))
        totalPoints  = user.points + pointsToAdd
        _           <- cuteGuardUserRepository.update(user.id, totalPoints.some, (pointsAddedToday + pointsToAdd).some)
        _           <- discordLogger.logToChannel(s"Added $pointsToAdd points to ${user.effectiveName}.")
        _           <- handleRanks(user, totalPoints, channel)
      yield ()

  def handleMember(member: Member, channel: Channel, maxPointsToAdd: Int): IO[Unit] =
    for
      user <- cuteGuardUserRepository.find(member.equalDiscordAndGuildID).getOrElseF(createNewUser(member))
      _    <- addPoints(user, channel, maxPointsToAdd)
    yield ()

object Points:
  extension (user: CuteGuardUser)
    def addRole(channel: Channel, rankForPoints: Role)(using
      logger: Logger[IO],
      discordLogger: DiscordLogger,
    ): IO[Unit] =
      for
        _      <- user.addRole(rankForPoints)
        message = s"${user.mention} has earned the rank ${rankForPoints.name}."
        _      <- channel.sendMessage(message)
        _      <- discordLogger.logToChannel(message)
      yield ()

    def removeRoles(roles: List[Role])(using Logger[IO]): IO[Unit] = roles.parTraverse_(user.removeRole)

  extension (ranks: List[(Int, Role)])
    def rankForPoints(points: Int): Option[Role] = ranks.sortBy(_(0)).foldLeft(None) {
      case (_, (pointsRequired, role)) if points >= pointsRequired => role.some
      case (maxRank, _)                                            => maxRank
    }

  def isToday(date: Instant): Boolean                    = isSameDay(date, Instant.now)
  def isSameDay(date1: Instant, date2: Instant): Boolean =
    val localDate1 = date1.atZone(ZoneId.systemDefault).toLocalDate
    val localDate2 = date2.atZone(ZoneId.systemDefault).toLocalDate
    localDate1.isEqual(localDate2)
