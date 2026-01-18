package cuteguard.commands

import cuteguard.*
import cuteguard.Epoch.*
import cuteguard.RelativeTime.given
import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.model.{Action, Event}
import cuteguard.model.discord.User
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashAPI, SlashCommandEvent}
import cuteguard.syntax.eithert.*
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
    _.addOption[Option[User]]("giver", "The giver of the last action, defaults to none."),
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
      giver     <- event.getOption[Option[User]]("giver").toEitherT
      _         <- EitherT.leftWhen(giver.contains(user), "The giver cannot be the same as the user.")
      events    <- EitherT.liftF(events.list(user.some, giver, action.some, None, "'last' command".some))
      mostRecent = events.maxByOption(_.date)
      guild     <- EitherT.fromOption(event.guild, "Could not get guild for last command.")
      givenBy    = giver.fold("")(giver => s" given by ${giver.getNameIn(guild)}")
      emptyText  = s"${user.mention} has no registered ${action.plural}$givenBy."
      text       = mostRecent.fold(emptyText)(event => s"${user.mention} last $action$givenBy was ${dateText(event)}")
    yield text
    eitherTResponse(response, slashAPI).void

  override val description: String = "Get the last date of the given action for you or the given user."
