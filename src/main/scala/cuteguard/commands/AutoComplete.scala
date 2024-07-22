package cuteguard.commands

import cuteguard.model.discord.event.AutoCompleteEvent
import cuteguard.utils.startsWithIgnoreCase

import cats.Show
import cats.effect.IO
import cats.syntax.applicative.*
import cats.syntax.show.*

import java.util.concurrent.TimeUnit

object AutoComplete:
  type AutoCompleteOption = (String, AutoCompleteEvent => IO[List[String]])
  val timeUnits: Map[String, TimeUnit] = Map(
    TimeUnit.SECONDS.toString.toLowerCase -> TimeUnit.SECONDS,
    TimeUnit.MINUTES.toString.toLowerCase -> TimeUnit.MINUTES,
    TimeUnit.HOURS.toString.toLowerCase   -> TimeUnit.HOURS,
    TimeUnit.DAYS.toString.toLowerCase    -> TimeUnit.DAYS,
  )

  lazy val timeUnit: AutoCompleteOption = "unit" -> (_ => timeUnits.keys.toList.pure)

sealed trait AutoCompletable[T: Show]:
  this: SlashCommand =>
  protected val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]]

  def matchesAutoComplete(event: AutoCompleteEvent): Boolean =
    event.fullCommand.equalsIgnoreCase(fullCommand)

  protected def focusedOptions(event: AutoCompleteEvent): IO[List[T]] =
    autoCompleteableOptions.get(event.focusedOption).fold(IO.pure(List.empty))(_(event))

  def apply(event: AutoCompleteEvent): IO[Unit] =
    focusedOptions(event).flatMap { autoCompleteableOptions =>
      event.replyChoices[String](autoCompleteableOptions.map(_.show).filter(_.startsWithIgnoreCase(event.focusedValue)))
    }

trait AutoComplete[T: Show] extends AutoCompletable[T]:
  this: SlashCommand =>
  val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[T]]]

  override protected val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] = autoCompleteOptions

trait AutoCompleteSimple[T: Show] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, IO[List[T]]]

  override protected val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions.view.mapValues(value => (_: AutoCompleteEvent) => value).toMap

trait AutoCompletePure[T: Show] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, AutoCompleteEvent => List[T]]

  override protected val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions.view.mapValues(_.andThen(IO.pure)).toMap

trait AutoCompleteSimplePure[T: Show] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, List[T]]

  override protected val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions.view.mapValues(value => (_: AutoCompleteEvent) => IO.pure(value)).toMap
