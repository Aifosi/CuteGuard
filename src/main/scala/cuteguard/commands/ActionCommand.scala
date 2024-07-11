package cuteguard.commands

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.{Channel, User}
import cuteguard.model.discord.event.SlashCommandEvent

import cats.effect.IO
import org.typelevel.log4cats.Logger

case class ActionCommand(events: Events, counterChanned: IO[Channel], action: Action) extends SlashCommand with Options:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean       = true
  override val fullCommand: String          = action.show
  override val options: List[PatternOption] = List(
    _.addOption[Int]("amount", s"How many ${action.plural} did you do?"),
    _.addOption[Option[User]]("giver", s"Who gave you these ${action.plural}."),
  )
  override val description: String          =
    s"Records a number of ${action.plural} you did, optionally add who gave them to you."

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val amount = event.getOption[Int]("amount")
    val giver  = event.getOption[Option[User]]("giver")

    counterChanned.flatMap {
      case counterChanned if event.channel.discordID != counterChanned.discordID =>
        event.replyEphemeral(s"This command can only be used in ${counterChanned.mention}.")
      case _ if amount <= 0                                                      =>
        event.replyEphemeral("Amount must be greater than 0.")
      case _ if giver.contains(event.author)                                     =>
        event.replyEphemeral(s"You cannot give yourself ${action.plural}.")
      case _                                                                     =>
        for
          _         <- events.add(event.author, giver, action, amount)
          actionText = if amount == 1 then action.show else action.plural
          givenBy    = giver.fold("")(giver => s" given by ${giver.mention}")
          _         <-
            event.reply(s"${event.author.mention} just did $amount $actionText$givenBy.")
        yield ()
    }.as(true)
