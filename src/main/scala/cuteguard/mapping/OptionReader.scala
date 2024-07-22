package cuteguard.mapping

type OptionResult[T] = Either[String, T]
trait OptionReader[+T]:
  def apply(string: String): OptionResult[T]

  def map[TT](f: T => TT): OptionReader[TT]                  = string => apply(string).map(f)
  def emap[TT](f: T => Either[String, TT]): OptionReader[TT] =
    string => apply(string).flatMap(f)

object OptionReader:
  def apply[T](using reader: OptionReader[T]) = reader

  def shouldNeverBeUsed(what: String): OptionReader[Nothing] = _ =>
    throw new Exception(s"Option Reader for $what is trying to be used!")

  private def error(string: String, t: String) = s"Unable to read $string as $t"

  given OptionReader[Int]     = string => string.toIntOption.toRight(error(string, "Int"))
  given OptionReader[Long]    = string => string.toLongOption.toRight(error(string, "Long"))
  given OptionReader[Double]  = string => string.toDoubleOption.toRight(error(string, "Double"))
  given OptionReader[Boolean] = string => string.toBooleanOption.toRight(error(string, "Boolean"))
  given OptionReader[String]  = Right(_)

  given optionReader[T](using reader: OptionReader[T]): OptionReader[Option[T]] = {
    case string: String if string.isBlank => Right(None)
    case string                           => reader(string).map(Some(_))
  }
