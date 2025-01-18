package cuteguard.commands

import cuteguard.*
import cuteguard.Epoch.*
import cuteguard.RelativeTime.given
import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.mapping.OptionWriter
import cuteguard.model.{Action, Event}
import cuteguard.model.discord.User
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashAPI, SlashCommandEvent}
import cuteguard.utils.toEitherT

import cats.Show
import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

case class Last(events: Events) extends SlashCommand with Options with AutoComplete[Action] with SlowResponse:
  override val fullCommand: String                                                     = "last"
  override val options: List[PatternOption]                                            = List(
    _.addOption[Action]("action", "Text action you want the date for.", autoComplete = true),
    _.addOption[Option[User]]("user", "Last date for whom, defaults to you."),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[Action]]] = Map(
    "action" -> Action.values.toList,
  ).fromSimplePure

  override val ephemeralResponses: Boolean = false

  private def dateText(event: Event): String = Show[RelativeTime].show(event.date.toEpoch.toRelativeTime)

  override def slowResponse(pattern: SlashPattern, event: SlashCommandEvent, slashAPI: Ref[IO, SlashAPI])(using
    Logger[IO],
  ): IO[Unit] =
    val response = for
      action    <- event.getOption[Action]("action").toEitherT
      user      <- event.getOption[Option[User]]("user").toEitherT.map(_.getOrElse(event.author))
      events    <- EitherT.liftF(events.list(user.some, None, action.some, None, "'last' command".some))
      mostRecent = events.maxByOption(_.date)
      emptyText  = s"${user.mention} has no ${action.plural} on record."
      text       = mostRecent.fold(emptyText)(event => s"${user.mention} last $action was ${dateText(event)}")
    yield text
    eitherTResponse(response, slashAPI).void

  override val description: String = "Get the last date of the given action for you or the given user."
