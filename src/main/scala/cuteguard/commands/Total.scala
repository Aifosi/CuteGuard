package cuteguard.commands

import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.User
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashAPI, SlashCommandEvent}
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.syntax.option.*
import cats.syntax.traverse.*
import org.typelevel.log4cats.Logger

case class Total(events: Events) extends SlashCommand with Options with AutoComplete[Action] with SlowResponse:
  override val fullCommand: String                                                     = "total"
  override val options: List[PatternOption]                                            = List(
    _.addOption[Option[Action]]("action", "Text action you want the total for.", autoComplete = true),
    _.addOption[Option[User]]("user", "Total for whom, defaults to you."),
    _.addOption[Option[User]]("giver", "Get total for actions given by this user."),
    _.addOption[Option[Int]](
      "last_days",
      "How many days in the past do you want highscores for. Default is 30 days.",
    ),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[Action]]] = Map(
    "action" -> Action.values.toList,
  ).fromSimplePure

  override val ephemeralResponses: Boolean = false

  override def slowResponse(pattern: SlashPattern, event: SlashCommandEvent, slashAPI: Ref[IO, SlashAPI])(using
    Logger[IO],
  ): IO[Unit] =
    val response = for
      action     <- event.getOption[Option[Action]]("action").toEitherT
      user       <- event.getOption[Option[User]]("user").toEitherT.map(_.getOrElse(event.author))
      giver      <- event.getOption[Option[User]]("giver").toEitherT
      lastDays   <- event.getOption[Option[Int]]("last_days").toEitherT.map(_.getOrElse(30))
      _          <- EitherT.leftWhen(lastDays <= 0, "`last_days` must be greater than 0!")
      events     <- EitherT.liftF(events.list(user.some, giver, action, lastDays.some))
      giverText   = giver.fold(".")(giver => s" given by ${giver.mention}.")
      days        = s"For the last $lastDays ${if lastDays == 1 then "day" else "days"} "
      start       = s"$days${user.mention} has a total of "
      textByEvent = events
                      .groupBy(_.action)
                      .view
                      .mapValues(_.map(_.amount).sum)
                      .toList
                      .map { case (action, total) =>
                        s"$total ${if total == 1 then action.show else action.plural}"
                      }
      emptyText   = s"$days ${user.mention} has no ${action.fold("events")(_.plural)} on record."
      text        = if textByEvent.isEmpty then emptyText else textByEvent.mkString(start, ", ", giverText)
    yield text
    eitherTResponse(response, slashAPI).void

  override val description: String = "Get the totals of the given action for you or the given user."
