package cuteguard.mapping

import cats.Show
import cats.syntax.show.*
trait OptionWritter[-T]:
  def apply(t: T): String

  def contramap[TT](f: TT => T): OptionWritter[TT] = thing => apply(f(thing))

object OptionWritter:
  def apply[T](using writer: OptionWritter[T]) = writer

  def shouldNeverBeUsed(what: String): OptionWritter[Any] = _ =>
    throw new Exception(s"Option Writter for $what is trying to be used!")

  given OptionWritter[Int]     = _.show
  given OptionWritter[Long]    = _.show
  given OptionWritter[Double]  = _.show
  given OptionWritter[Boolean] = _.show
  given OptionWritter[String]  = identity(_)

  given optionWritter[T](using writer: OptionWritter[T]): OptionWritter[Option[T]] = _.fold("")(writer.apply)
