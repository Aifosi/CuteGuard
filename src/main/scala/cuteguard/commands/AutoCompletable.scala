package cuteguard.commands

import cuteguard.mapping.OptionWriter
import cuteguard.model.discord.event.AutoCompleteEvent
import cuteguard.syntax.string.*

import cats.effect.IO

object AutoCompletable:
  extension [T](map: Map[String, List[T]])
    def fromSimplePure: Map[String, AutoCompleteEvent => IO[List[T]]] =
      map.view.mapValues(value => (_: AutoCompleteEvent) => IO.pure(value)).toMap
  extension [T](map: Map[String, IO[List[T]]])
    def fromSimple: Map[String, AutoCompleteEvent => IO[List[T]]]     =
      map.view.mapValues(value => (_: AutoCompleteEvent) => value).toMap
  extension [T](map: Map[String, AutoCompleteEvent => List[T]])
    def fromPure: Map[String, AutoCompleteEvent => IO[List[T]]]       =
      map.view.mapValues(_.andThen(IO.pure)).toMap

trait AutoCompletable[T: OptionWriter as writer]:
  this: SlashCommand =>

  protected def autoCompleteOptionsGen: Map[String, AutoCompleteEvent => IO[List[T]]]

  def matchesAutoComplete(event: AutoCompleteEvent): Boolean =
    event.fullCommand.equalsIgnoreCase(fullCommand)

  protected def focusedOptions(event: AutoCompleteEvent): IO[List[T]] =
    autoCompleteOptionsGen.get(event.focusedOption).fold(IO.pure(List.empty))(_(event))

  private def filteredOptions(event: AutoCompleteEvent): IO[List[T]] =
    focusedOptions(event).map(_.flatMap { option =>
      Option.when(writer(option).startsWithIgnoreCase(event.focusedValue))(option)
    }.take(25))

  protected inline def reply(event: AutoCompleteEvent): IO[Boolean] =
    filteredOptions(event).flatMap(event.replyChoices[T]).as(true)

  def apply(event: AutoCompleteEvent): IO[Boolean]

trait AutoComplete[T] extends AutoCompletable[T]:
  this: SlashCommand =>

  val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[T]]]

  override protected lazy val autoCompleteOptionsGen: Map[String, AutoCompleteEvent => IO[List[T]]] =
    autoCompleteOptions

  override def apply(event: AutoCompleteEvent): IO[Boolean] = reply(event)

trait AutoCompleteInt extends AutoCompletable[Int]:
  this: SlashCommand =>

  val autoCompleteInts: Map[String, AutoCompleteEvent => IO[List[Int]]]

  override protected lazy val autoCompleteOptionsGen: Map[String, AutoCompleteEvent => IO[List[Int]]] = autoCompleteInts

  override def apply(event: AutoCompleteEvent): IO[Boolean] = reply(event)

trait AutoCompleteLong extends AutoCompletable[Long]:
  this: SlashCommand =>

  val autoCompleteLongs: Map[String, AutoCompleteEvent => IO[List[Long]]]

  override protected lazy val autoCompleteOptionsGen: Map[String, AutoCompleteEvent => IO[List[Long]]] =
    autoCompleteLongs

  override def apply(event: AutoCompleteEvent): IO[Boolean] = reply(event)

trait AutoCompleteDouble extends AutoCompletable[Double]:
  this: SlashCommand =>

  val autoCompleteDoubles: Map[String, AutoCompleteEvent => IO[List[Double]]]

  override protected lazy val autoCompleteOptionsGen: Map[String, AutoCompleteEvent => IO[List[Double]]] =
    autoCompleteDoubles

  override def apply(event: AutoCompleteEvent): IO[Boolean] = reply(event)
