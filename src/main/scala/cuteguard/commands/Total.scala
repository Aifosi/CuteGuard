package cuteguard.commands

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.User
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashCommandEvent}

import cats.effect.IO
import cats.syntax.applicative.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import org.typelevel.log4cats.Logger

case class Total(events: Events) extends SlashCommand with Options with AutoCompleteString:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean                                                  = true
  override val fullCommand: String                                                     = "total"
  override val options: List[PatternOption]                                            = List(
    _.addOption[Option[String]]("action", "Text action you want the total for.", true),
    _.addOption[Option[User]]("user", "Total for whom, defaults to you."),
    _.addOption[Option[User]]("giver", "Get total for actions given by this user."),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[String]]] = Map(
    "action" -> (_ => Action.values.toList.map(_.show).pure),
  )

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val action = event
      .getOption[Option[String]]("action")
      .traverse(Action.fromString)
    val user   = event.getOption[Option[User]]("user").getOrElse(event.author)
    val giver  = event.getOption[Option[User]]("giver")

    action
      .fold(
        event.replyEphemeral,
        action =>
          for
            events   <- events.list(user.some, giver, action)
            giverText = giver.fold(".")(giver => s" given by ${giver.mention}.")
            start     = s"${user.mention} has a total of "
            text      = events
                          .groupBy(_.action)
                          .view
                          .mapValues(_.map(_.amount).sum)
                          .toList
                          .map { case (action, total) =>
                            s"$total ${if total == 1 then action.show else action.plural}"
                          }
                          .mkString(start, ", ", giverText)
            _        <- event.reply(text)
          yield (),
      )
      .as(true)

  override val description: String = "Get the totals of the given action for you or the given user."
