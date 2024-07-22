package cuteguard.commands

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.User
import cuteguard.model.discord.event.SlashCommandEvent
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.option.*
import cats.syntax.traverse.*
import org.typelevel.log4cats.Logger

case class Total(events: Events)
    extends SlashCommand with Options with AutoCompleteSimplePure[String] with ErrorMessages:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean                         = true
  override val fullCommand: String                            = "total"
  override val options: List[PatternOption]                   = List(
    _.addOption[Option[Action]]("action", "Text action you want the total for.", autoComplete = true),
    _.addOption[Option[User]]("user", "Total for whom, defaults to you."),
    _.addOption[Option[User]]("giver", "Get total for actions given by this user."),
    _.addOption[Option[Int]](
      "last_days",
      "How many days in the past do you want highscores for. Default is the whole history",
    ),
  )
  override val autoCompleteOptions: Map[String, List[String]] = Map(
    "action" -> Action.values.toList.map(_.show),
  )

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      action   <- event.getOption[Option[Action]]("action").toEitherT
      _         = println(action)
      user     <- event.getOption[Option[User]]("user").toEitherT.map(_.getOrElse(event.author))
      giver    <- event.getOption[Option[User]]("giver").toEitherT
      lastDays <- event.getOption[Option[Int]]("last_days").toEitherT
      _        <- EitherT.leftWhen(lastDays.exists(_ <= 0), "`last_days` must be greater than 0!")
      events   <- EitherT.liftF(events.list(user.some, giver, action, lastDays))
      giverText = giver.fold(".")(giver => s" given by ${giver.mention}.")
      days      = lastDays.fold("")(lastDays => s"For the last $lastDays ")
      start     = s"$days${user.mention} has a total of "
      text      = events
                    .groupBy(_.action)
                    .view
                    .mapValues(_.map(_.amount).sum)
                    .toList
                    .map { case (action, total) =>
                      s"$total ${if total == 1 then action.show else action.plural}"
                    }
                    .mkString(start, ", ", giverText)
      _        <- EitherT.liftF(event.reply(text))
    yield true

  override val description: String = "Get the totals of the given action for you or the given user."
