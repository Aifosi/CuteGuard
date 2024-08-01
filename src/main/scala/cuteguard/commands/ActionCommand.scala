package cuteguard.commands

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.{Channel, User}
import cuteguard.model.discord.event.SlashCommandEvent
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import org.typelevel.log4cats.Logger

case class ActionCommand(events: Events, counterChanned: IO[Channel], action: Action)
    extends SlashCommand with Options with ErrorMessages:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean       = true
  override val fullCommand: String          = action.show
  override val options: List[PatternOption] = List(
    _.addOption[Option[Int]]("amount", s"How many ${action.plural} did you do? Defaults to 1."),
    _.addOption[Option[User]]("giver", s"Who gave you these ${action.plural}."),
  )
  override val description: String          =
    s"Records a number of ${action.plural} you did, optionally add who gave them to you."

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      counterChanned <- EitherT.liftF(counterChanned)
      _              <-
        EitherT
          .leftWhen(event.channel != counterChanned, s"This command can only be used in ${counterChanned.mention}.")
      amount         <- event.getOption[Option[Int]]("amount").toEitherT.map(_.getOrElse(1))
      _              <- EitherT.leftWhen(amount <= 0, "Amount must be greater than 0.")
      giver          <- event.getOption[Option[User]]("giver").toEitherT
      _              <- EitherT.leftWhen(giver.contains(event.author), s"You cannot give yourself ${action.plural}.")
      _              <- EitherT.liftF(events.add(event.author, giver, action, amount))
      actionText      = if amount == 1 then action.show else action.plural
      givenBy         = giver.fold("")(giver => s" given by ${giver.mention}")
      _              <-
        EitherT.liftF(event.reply(s"${event.author.mention} just did $amount $actionText$givenBy."))
    yield true
