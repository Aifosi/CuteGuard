package cuteguard.commands

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.{Member, User}
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashCommandEvent}
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

case class Highscore(events: Events)
    extends SlashCommand with Options with AutoCompleteSimplePure[String] with ErrorMessages:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean                         = true
  override val fullCommand: String                            = "highscore"
  private val topDefault                                      = 10
  override val options: List[PatternOption]                   = List(
    _.addOption[Action]("action", "Text action you want the total for.", autoComplete = true),
    _.addOption[Option[Int]]("top", s"How many positions from the top to show. Default is $topDefault"),
    _.addOption[Option[Int]](
      "last_days",
      "How many days in the past do you want highscores for. Default is the whole history",
    ),
  )
  override val autoCompleteOptions: Map[String, List[String]] = Map(
    "action" -> Action.values.toList.map(_.show),
  )

  extension (int: Int)
    def padWithThousandsSeparator(max: Int): String =
      val size = max.toString.grouped(3).mkString(" ").length
      int.toString.grouped(3).mkString(" ").reverse.padTo(size, ' ').reverse

  def highscoreText(topEvents: List[((Member, Int), Int)], action: Action, author: User, lastDays: Option[Int]) =
    val days  = lastDays.fold("")(lastDays => s" for the last $lastDays")
    val start = s"Current highscore for **${action.show}**$days is:\n"
    topEvents.map { case ((member, total), top) =>
      val totalText = total.padWithThousandsSeparator(topEvents.head(0)(1))
      val topText   = (top + 1).padWithThousandsSeparator(topEvents.size)
      s"`$topText. $totalText - ${member.guildName}`"
    }
      .mkString(start, "\n", "")

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      action   <- event.getOption[Action]("action").toEitherT
      top      <- event.getOption[Option[Int]]("top").toEitherT.map(_.getOrElse(topDefault))
      lastDays <- event.getOption[Option[Int]]("last_days").toEitherT
      _        <- EitherT.leftWhen(lastDays.exists(_ <= 0), "`last_days` must be greater than 0!")
      events   <- EitherT.liftF(events.list(None, None, action.some, lastDays))
      topEvents = events
                    .groupBy(_.receiver)
                    .view
                    .mapValues(_.map(_.amount).sum)
                    .toList
                    .sortBy(_(1))(Ordering[Int].reverse)
                    .take(top)
                    .zipWithIndex

      text = if topEvents.isEmpty then s"There are no entries for **${action.show}**."
             else highscoreText(topEvents, action, event.author, lastDays)
      _   <- EitherT.liftF(event.reply(text))
    yield true

  override val description: String = "Get the highscore for the given action."
