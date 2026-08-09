package cuteguard.commands

import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashAPI, SlashCommandEvent}
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

case class Given(events: Events)
    extends SlashCommand with Options with AutoComplete[Action] with SlowResponse with FunnyCommand:
  override val fullCommand: String                                                     = "given"
  private val topDefault                                                               = 10
  override val options: List[PatternOption]                                            = List(
    _.addOption[Action]("action", "Text action you want the total for.", autoComplete = true),
    _.addOption[Option[Int]]("top", s"How many positions from the top to show. Default is $topDefault, 0 shows all."),
    _.addOption[Option[Int]](
      "last_days",
      "How many days in the past do you want giver scores for. Default is 30 days.",
    ),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[Action]]] = Map(
    "action" -> Action.values.toList,
  ).fromSimplePure

  override val ephemeralResponses: Boolean = false

  override def slowResponse(
    pattern: SlashPattern,
    event: SlashCommandEvent,
    slashAPI: Ref[IO, SlashAPI],
  )(using Logger[IO]): IO[Unit] =
    val response = for
      action    <- event.getOption[Action]("action").toEitherT
      top       <- event.getOption[Option[Int]]("top").toEitherT.map(_.getOrElse(topDefault))
      lastDays  <- event.getOption[Option[Int]]("last_days").toEitherT.map(_.getOrElse(30))
      _         <- EitherT.leftWhen(lastDays <= 0, "`last_days` must be greater than 0!")
      eventList <- EitherT.liftF(events.lowList(None, None, action.some, lastDays.some))
      highScores = eventList.collect { case (_, _, _, Some(giver_user_uuid), Some(giver_user_id), _, amount, _) =>
                     (giver_user_uuid, giver_user_id, amount)
                   }

      daysText = s" for the last $lastDays ${if lastDays == 1 then "day" else "days"}"
      text    <-
        highScores.headOption.fold(EitherT.pure[IO, String](s"There are no entries for **${action.show}**$daysText.")) {
          _ =>
            HighScore.formatted(events.users.guild, highScores, top, action, daysText)
        }
    yield text
    eitherTResponse(response, slashAPI).void

  override val description: String = "Get the high scores for the givers for the chosen action."
