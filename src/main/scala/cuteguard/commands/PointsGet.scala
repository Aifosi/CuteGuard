package cuteguard.commands

import cuteguard.db.CuteGuardUserRepository
import cuteguard.db.Filters.*
import cuteguard.model.User
import cuteguard.model.event.SlashCommandEvent

import cats.data.EitherT
import cats.effect.IO
import org.typelevel.log4cats.Logger

class PointsGet(cuteGuardUserRepository: CuteGuardUserRepository) extends SlashCommand with Options:

  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean       = true
  override val fullCommand: String          = "points get"
  override val options: List[PatternOption] = List(
    _.addOption[User]("user", "User to get the points."),
  )

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val user = event.getOption[User]("user")

    (for
      guild <- EitherT.liftF(event.guild)
      user  <- cuteGuardUserRepository
                 .find(user.discordID.equalDiscordID, guild.discordID.equalGuildID)
                 .toRight("Failed to find user.")
    yield s"${user.mention} has ${user.points} points, ${user.pointsToday} of which were earned today.").merge
      .flatMap(event.replyEphemeral)
      .as(true)

  override val description: String = "Shows how many points a user has."
