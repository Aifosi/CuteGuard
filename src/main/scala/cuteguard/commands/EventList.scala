package cuteguard.commands

import cuteguard.EventEditor
import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashCommandEvent}
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

case class EventList(events: Events, eventEditor: EventEditor)
    extends SlashCommand with Options with AutoComplete[Action] with ErrorMessages:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean = true
  override val fullCommand: String    = "event list"

  override val options: List[PatternOption]                                            = List(
    _.addOption[Action]("action", "Action you to list events for.", autoComplete = true),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[Action]]] = Map(
    "action" -> Action.values.toList,
  ).fromSimplePure

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      action         <- event.getOption[Action]("action").toEitherT
      events         <- EitherT.liftF(events.list(event.author.some, None, action.some, None))
      _              <- EitherT.leftWhen(events.isEmpty, s"You have no ${action.plural} on record.")
      eventMap        = events.zipWithIndex.toMap.map(_.swap)
      _              <- EitherT.liftF(eventEditor.registerActiveEdit(event.author, eventMap))
      formattedEvents = EventEditor.formatEvents(eventMap)
      editMessage     = "You can edit the following events for the next 10 minutes\n"
      _              <- EitherT.liftF(event.replyEphemeral(editMessage + formattedEvents))
    yield true

  override val description: String = "Lists actions history to be edited or deleted."