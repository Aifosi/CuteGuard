package cuteguard.mapping

import cats.Show
import cats.syntax.show.*
trait OptionWriter[T]:
  def apply(t: T): String

  def contramap[TT](f: TT => T): OptionWriter[TT] = thing => apply(f(thing))

object OptionWriter:
  def apply[T](using writer: OptionWriter[T]) = writer

  def shouldNeverBeUsed[T](what: String): OptionWriter[T] = _ =>
    throw new Exception(s"Option Writter for $what is trying to be used!")

  given OptionWriter[Int]     = _.show
  given OptionWriter[Long]    = _.show
  given OptionWriter[Double]  = _.show
  given OptionWriter[Boolean] = _.show
  given OptionWriter[String]  = identity(_)

  given optionWritter[T](using writer: OptionWriter[T]): OptionWriter[Option[T]] = _.fold("")(writer.apply)
