package cuteguard.commands

import cuteguard.EventEditor
import cuteguard.db.Events
import cuteguard.model.discord.event.SlashCommandEvent
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import org.typelevel.log4cats.Logger

case class EventDelete(events: Events, eventEditor: EventEditor) extends SlashCommand with Options with ErrorMessages:
  override val fullCommand: String    = "event delete"

  override val options: List[PatternOption] = List(
    _.addOption[Int]("id", "Text action you want the total for."),
  )

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      id         <- event.getOption[Int]("id").toEitherT
      activeEdit <- OptionT(eventEditor.activeEdit(event.author))
                      .toRight("You have no active edits, use `/event list` first.")
      _          <- EitherT.leftWhen(!activeEdit.keySet.contains(id), s"Invalid ID: $id")
      _          <- EitherT.liftF(events.delete(activeEdit(id).id))
      _          <- EitherT.liftF(eventEditor.registerActiveEdit(event.author, activeEdit - id))
      _          <- EitherT.liftF(event.replyEphemeral("Event deleted."))
    yield true

  override val description: String = "Edits action history"
