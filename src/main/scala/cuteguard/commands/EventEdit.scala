package cuteguard.commands

import cuteguard.EventEditor
import cuteguard.db.Events
import cuteguard.model.discord.User
import cuteguard.model.discord.event.SlashCommandEvent
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import org.typelevel.log4cats.Logger

class EventEdit(events: Events, eventEditor: EventEditor) extends SlashCommand with Options with ErrorMessages:
  override val fullCommand: String    = "event edit"

  override val options: List[PatternOption] = List(
    _.addOption[Int]("id", "Text action you want the total for."),
    _.addOption[Option[User]]("giver", "The giver to chage to."),
    _.addOption[Option[Int]]("amount", "The new amount"),
    _.addOption[Option[Boolean]]("remove_giver", "Do you want to remove the giver?"),
  )

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      id          <- event.getOption[Int]("id").toEitherT
      activeEdit  <- OptionT(eventEditor.activeEdit(event.author))
                       .toRight("You have no active edits, use `/event list` first.")
      _           <- EitherT.leftWhen(!activeEdit.keySet.contains(id), s"Invalid ID: $id")
      giver       <- event.getOption[Option[User]]("giver").toEitherT
      amount      <- event.getOption[Option[Int]]("amount").toEitherT
      _           <- EitherT.leftWhen(amount.exists(_ <= 0), "Amount must be greater than 0.")
      removeGiver <- event.getOption[Option[Boolean]]("remove_giver").toEitherT
      _           <- EitherT.leftWhen(
                       removeGiver.isDefined && giver.isDefined,
                       "Can't both define `remove_giver` and `giver`!",
                     )
      _           <- EitherT.leftWhen(
                       List(giver, amount, removeGiver).flatten.isEmpty,
                       "Please choose what you want to edit from this event.",
                     )
      finalGiver   = Option.when(removeGiver.contains(true) || giver.isDefined)(giver)
      _           <- EitherT.liftF(
                       events
                         .edit(activeEdit(id).id, finalGiver, amount, None, Some("Edit event user command"))
                         .foldF(Logger[IO].debug("Update failed."))(_ => IO.unit),
                     )
      _           <- EitherT.liftF(eventEditor.registerActiveEdit(event.author, activeEdit))
      _           <- EitherT.liftF(event.replyEphemeral("Event edited."))
    yield true

  override val description: String = "Edits action history"
