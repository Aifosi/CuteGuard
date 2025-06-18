package cuteguard.commands

import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.mapping.OptionWriter
import cuteguard.model.Action
import cuteguard.model.discord.DiscordID
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashAPI, SlashCommandEvent}
import cuteguard.syntax.chaining.*
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

import java.util.UUID

case class Highscore(events: Events) extends SlashCommand with Options with AutoComplete[Action] with SlowResponse:
  override val fullCommand: String                                                     = "highscore"
  private val topDefault                                                               = 10
  override val options: List[PatternOption]                                            = List(
    _.addOption[Action]("action", "Text action you want the total for.", autoComplete = true),
    _.addOption[Option[Int]]("top", s"How many positions from the top to show. Default is $topDefault, 0 shows all."),
    _.addOption[Option[Int]](
      "last_days",
      "How many days in the past do you want high scores for. Default is 30 days.",
    ),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[Action]]] = Map(
    "action" -> Action.values.toList,
  ).fromSimplePure

  extension (int: Int)
    private def withThousandsSeparatorReverse: String = int.toString.reverse.grouped(3).mkString(" ")
    def withThousandsSeparator: String                = withThousandsSeparatorReverse.reverse
    def padWithThousandsSeparator(size: Int): String  =
      withThousandsSeparatorReverse.padTo(size, ' ').reverse

  def highscoreText(
    topEvents: List[(((UUID, DiscordID), Int), Int)],
    action: Action,
    daysText: String,
  )(using Logger[IO]) =
    val discordIDToUUIDs = topEvents.map { case (((uuid, discordID), _), _) => discordID -> uuid }.toMap

    events.users.guild
      .flatMap(_.members(discordIDToUUIDs.keySet))
      .map { members =>
        val start            = s"Current highscore for **${action.show}**$daysText is:\n"
        val maxTotalTextSize = topEvents.head(0)(1).withThousandsSeparator.length
        val maxTextSize      = topEvents.size.withThousandsSeparator.length
        val uuidToName       = members
          .map(member => discordIDToUUIDs(member.discordID) -> member.guildName)
          .toMap
          .withDefaultValue("Member left server")
        topEvents.map { case (((uuid, _), total), top) =>
          val totalText = total.padWithThousandsSeparator(maxTotalTextSize)
          val topText   = (top + 1).padWithThousandsSeparator(maxTextSize)
          s"`$topText. $totalText - ${uuidToName(uuid)}`"
        }.mkString(start, "\n", "")
      }
      .leftSemiflatTap(Logger[IO].error(_)("Failed to list users"))
      .leftMap(_ => "Failed to get user names, sorry!")

  override val ephemeralResponses: Boolean = false

  override def slowResponse(
    pattern: SlashPattern,
    event: SlashCommandEvent,
    slashAPI: Ref[IO, SlashAPI],
  )(using Logger[IO]): IO[Unit] =
    val response = for
      action   <- event.getOption[Action]("action").toEitherT
      top      <- event.getOption[Option[Int]]("top").toEitherT.map(_.getOrElse(topDefault))
      lastDays <- event.getOption[Option[Int]]("last_days").toEitherT.map(_.getOrElse(30))
      _        <- EitherT.leftWhen(lastDays <= 0, "`last_days` must be greater than 0!")
      events   <- EitherT.liftF(events.lowList(None, None, action.some, lastDays.some))
      topEvents = events
                    .map((_, receiver_user_uuid, receiver_user_id, _, _, _, amount, _) =>
                      (receiver_user_uuid, receiver_user_id, amount),
                    )
                    .groupBy((receiver_user_uuid, receiver_user_id, _) => (receiver_user_uuid, receiver_user_id))
                    .view
                    .mapValues(_.map(_(2)).sum)
                    .toList
                    .sortBy(_(1))(Ordering[Int].reverse)
                    .when(top > 0)(_.take(top))
                    .zipWithIndex

      daysText = s" for the last $lastDays ${if lastDays == 1 then "day" else "days"}"
      text    <-
        topEvents.headOption.fold(EitherT.pure[IO, String](s"There are no entries for **${action.show}**$daysText.")) {
          _ =>
            highscoreText(topEvents, action, daysText)
        }
    yield text
    eitherTResponse(response, slashAPI).void

  override val description: String = "Get the highscore for the given action."
