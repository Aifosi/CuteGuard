package cuteguard.commands

import cuteguard.*
import cuteguard.RelativeTime.given
import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Events
import cuteguard.mapping.OptionWriter
import cuteguard.model.Action
import cuteguard.model.discord.{Channel, User}
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashCommandEvent}
import cuteguard.syntax.eithert.*
import cuteguard.syntax.localdate.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.show.given
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.typelevel.log4cats.Logger

import java.time.{LocalDate, YearMonth}

case class ActionCommand(events: Events, counterChannel: IO[Channel], action: Action)
    extends SlashCommand with Options with ErrorMessages with AutoCompleteInt:
  override val description: String =
    s"Records a number of ${action.plural} you did, optionally add who gave them to you."

  override val fullCommand: String          = action.show
  override val options: List[PatternOption] = List(
    _.addOption[Option[Int]]("amount", s"How many ${action.plural} did you do? Defaults to 1."),
    _.addOption[Option[User]]("giver", s"Who gave you these ${action.plural}."),
    _.addOption[Option[Int]](
      "year",
      "The year you did it, if not today. Defaults to current year.",
      autoComplete = true,
    ),
    _.addOption[Option[Int]](
      "month",
      "The month you did it, if not today. Defaults to current month.",
      autoComplete = true,
    ),
    _.addOption[Option[Int]]("day", "The day you did it, if not today. Defaults to current day.", autoComplete = true),
  )

  extension (event: AutoCompleteEvent)
    private def getOption(name: String): Option[Int] = event.options.collectFirst {
      case option if option.getName.equalsIgnoreCase(name) => option.getAsInt
    }
    private def getDaysForMonth: List[Int]           =
      val now   = LocalDate.now
      val year  = event.getOption("year").getOrElse(now.getYear)
      val month = event.getOption("month").getOrElse(now.getMonthValue)
      if year == now.getYear && month == now.getMonthValue then List.range(1, now.getDayOfMonth + 1)
      else List.range(1, YearMonth.of(year, month).lengthOfMonth + 1)

  override val autoCompleteInts: Map[String, AutoCompleteEvent => IO[List[Int]]] =
    Map[String, AutoCompleteEvent => List[Int]](
      "year"  -> (_ => List.range(2023, LocalDate.now.getYear + 1)),
      "month" -> (_ => List.range(1, 12 + 1)),
      "day"   -> (_.getDaysForMonth),
    ).fromPure

  private def getDate(event: SlashCommandEvent): Either[String, Option[LocalDate]] =
    for
      year  <- event.getOption[Option[Int]]("year")
      month <- event.getOption[Option[Int]]("month")
      day   <- event.getOption[Option[Int]]("day")
    yield (year, month, day) match
      case (None, None, None) => None
      case (year, month, day) =>
        val now  = LocalDate.now
        val date = LocalDate
          .of(
            year.getOrElse(now.getYear),
            month.getOrElse(now.getMonthValue),
            day.getOrElse(now.getDayOfMonth),
          )
        Option.unless(now == date)(date)

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      counterChan <- EitherT.liftF(counterChannel)
      _           <- EitherT.leftWhen(
                       event.channel != counterChan,
                       s"This command can only be used in ${counterChan.mention}.",
                     )
      amount      <- event.getOption[Option[Int]]("amount").toEitherT.map(_.getOrElse(1))
      _           <- EitherT.leftWhen(amount <= 0, "Amount must be greater than 0.")
      date        <- getDate(event).toEitherT
      _           <- EitherT.leftWhen(
                       date.exists(_.isAfter(LocalDate.now)),
                       s"Cannot add ${action.plural} in the future!",
                     )
      giver       <- event.getOption[Option[User]]("giver").toEitherT
      _           <- EitherT.leftWhen(giver.contains(event.author), s"You cannot give yourself ${action.plural}.")
      _           <- EitherT.liftF(events.add(event.author, giver, action, amount, date))
      actionText   = if amount == 1 then action.show else action.plural
      givenBy      = giver.fold("")(giver => s" given by ${giver.mention}")
      message      = date.fold(s"${event.author.mention} just did $amount $actionText$givenBy.") { date =>
                       show"${event.author.mention} just added $amount $actionText$givenBy done ${date.toRelativeTime}."
                     }
      _           <- EitherT.liftF(event.reply(message))
    yield true
