package cuteguard.commands

import cuteguard.db.CuteGuardUserRepository
import cuteguard.db.Filters.*
import cuteguard.model.User
import cuteguard.model.event.SlashCommandEvent

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

class PointsSet(cuteGuardUserRepository: CuteGuardUserRepository) extends SlashCommand with Options:

  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean       = false
  override val fullCommand: String          = "points set"
  override val options: List[PatternOption] = List(
    _.addOption[User]("user", "User to set the points of."),
    _.addOption[Int]("points", "Number of points to set."),
  )

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val user   = event.getOption[User]("user")
    val points = event.getOption[Int]("points")

    (for
      guild       <- EitherT.liftF(event.guild)
      user        <- cuteGuardUserRepository
                       .find(user.discordID.equalDiscordID, guild.discordID.equalGuildID)
                       .toRight("Failed to find user.")
      updatedUser <- EitherT.liftF(cuteGuardUserRepository.update(user.id, points.some))
    yield s"${updatedUser.mention} now has ${updatedUser.points} points.").merge
      .flatMap(event.replyEphemeral)
      .as(true)

  override val description: String = "Sets a user points."
