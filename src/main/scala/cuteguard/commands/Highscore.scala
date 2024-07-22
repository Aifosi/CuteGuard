package cuteguard.commands

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.{Member, User}
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashCommandEvent}

import cats.effect.IO
import cats.syntax.applicative.*
import cats.syntax.option.*
import org.typelevel.log4cats.Logger

import scala.util.chaining.*

case class Highscore(events: Events) extends SlashCommand with Options with AutoCompletePure[String]:
  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean                                              = true
  override val fullCommand: String                                                 = "highscore"
  private val topDefault                                                           = 10
  override val options: List[PatternOption]                                        = List(
    _.addOption[String]("action", "Text action you want the total for.", autoComplete = true),
    _.addOption[Option[Int]]("top", s"How many positions from the top to show. Default is $topDefault"),
    _.addOption[Option[Int]](
      "last_days",
      "How many days in the past do you want highscores for. Default is the whole history",
    ),
  )
  override val autoCompleteOptions: Map[String, AutoCompleteEvent => List[String]] = Map(
    "action" -> (_ => Action.values.toList.map(_.show)),
  )

  extension (int: Int)
    def padWithThousandsSeparator(max: Int): String =
      val size = max.toString.grouped(3).mkString(" ").length
      int.toString.grouped(3).mkString(" ").reverse.padTo(size, ' ').reverse

  def highscoreText(topEvents: List[((Member, Int), Int)], action: Action, author: User) =
    val start = s"Current highscore for **${action.show}** is:\n"
    topEvents.map { case ((member, total), top) =>
      val totalText = total.padWithThousandsSeparator(topEvents.head(0)(1))
      val topText   = (top + 1).padWithThousandsSeparator(topEvents.size)
      s"  `$topText.` `$totalText` - ${if author == member then member.mention else member.guildName}"
    }
      .mkString(start, "\n", "")

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val action = event
      .getOption[String]("action")
      .pipe(Action.fromString)
    val top    = event.getOption[Option[Int]]("top").getOrElse(topDefault)

    action
      .fold(
        event.replyEphemeral,
        action =>
          for
            events   <- events.list(None, None, action.some)
            topEvents = events
                          .groupBy(_.receiver)
                          .view
                          .mapValues(_.map(_.amount).sum)
                          .toList
                          .sortBy(_(1))(Ordering[Int].reverse)
                          .take(top)
                          .zipWithIndex

            text = if topEvents.isEmpty then s"There are no entries for **${action.show}**."
                   else highscoreText(topEvents, action, event.author)
            _   <- event.reply(text)
          yield (),
      )
      .as(true)

  override val description: String = "Get the highscore for the given action."
