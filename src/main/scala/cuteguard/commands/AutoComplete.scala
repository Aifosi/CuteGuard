package cuteguard.commands

import cuteguard.mapping.OptionWritter
import cuteguard.model.discord.event.AutoCompleteEvent
import cuteguard.syntax.action.*
import cuteguard.syntax.string.*

import cats.effect.IO
import cats.syntax.applicative.*

import java.util.concurrent.TimeUnit
import scala.compiletime.{asMatchable, erasedValue}

object AutoComplete:
  type AutoCompleteOption = (String, AutoCompleteEvent => IO[List[String]])
  val timeUnits: Map[String, TimeUnit] = Map(
    TimeUnit.SECONDS.toString.toLowerCase -> TimeUnit.SECONDS,
    TimeUnit.MINUTES.toString.toLowerCase -> TimeUnit.MINUTES,
    TimeUnit.HOURS.toString.toLowerCase   -> TimeUnit.HOURS,
    TimeUnit.DAYS.toString.toLowerCase    -> TimeUnit.DAYS,
  )

  lazy val timeUnit: AutoCompleteOption = "unit" -> (_ => timeUnits.keys.toList.pure)

trait AutoCompletable[T](using val writter: OptionWritter[T]):
  this: SlashCommand =>
  protected lazy val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]]

  def matchesAutoComplete(event: AutoCompleteEvent): Boolean =
    event.fullCommand.equalsIgnoreCase(fullCommand)

  inline def focusedOptions(event: AutoCompleteEvent): IO[List[T]] =
    autoCompleteableOptions.get(event.focusedOption).fold(IO.pure(List.empty))(_(event))

trait AutoComplete[T: OptionWritter] extends AutoCompletable[T]:
  this: SlashCommand =>
  val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[T]]]

  override protected lazy val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions

trait AutoCompleteSimple[T: OptionWritter] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, IO[List[T]]]

  override protected lazy val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions.view.mapValues(value => (_: AutoCompleteEvent) => value).toMap

trait AutoCompletePure[T: OptionWritter] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, AutoCompleteEvent => List[T]]

  override protected lazy val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions.view.mapValues(_.andThen(IO.pure)).toMap

trait AutoCompleteSimplePure[T: OptionWritter] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, List[T]]

  override protected lazy val autoCompleteableOptions: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions.view.mapValues(value => (_: AutoCompleteEvent) => IO.pure(value)).toMap
