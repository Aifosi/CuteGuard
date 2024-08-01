package cuteguard.mapping

import cats.Show
import cats.syntax.show.*
import cuteguard.model.discord.event.AutoCompleteEvent
trait OptionWritter[T]:
  def apply(t: T, event: AutoCompleteEvent): String

  def contramap[TT](f: TT => T): OptionWritter[TT] = thing => apply(f(thing))

object OptionWritter:
  def apply[T](using writer: OptionWritter[T]) = writer

  def filteredOptions[T](writter: OptionWritter[T], event: AutoCompleteEvent, options: List[T]): List[T] = options.flatMap { option =>
    Option.when(writter(option).startsWithIgnoreCase(event.focusedValue))(option)
  }

  def shouldNeverBeUsed[T](what: String): OptionWritter[T] = _ =>
    throw new Exception(s"Option Writter for $what is trying to be used!")

  given OptionWritter[Int]     = _.show
  given OptionWritter[Long]    = _.show
  given OptionWritter[Double]  = _.show
  given OptionWritter[Boolean] = _.show
  given OptionWritter[String]  = identity(_)

  given optionWritter[T](using writer: OptionWritter[T]): OptionWritter[Option[T]] = _.fold("")(writer.apply)
