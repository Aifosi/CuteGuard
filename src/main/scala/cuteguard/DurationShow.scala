package cuteguard

import cats.Show

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

object DurationShow:
  private val abbreviate = Map(
    NANOSECONDS  -> "ns",
    MICROSECONDS -> "μs",
    MILLISECONDS -> "ms",
    SECONDS      -> "s",
    MINUTES      -> "min",
    HOURS        -> "h",
    DAYS         -> "d",
  )

  private val units = TimeUnit.values.reverse

  given Show[Duration] = {
    case duration: FiniteDuration =>
      val chosenUnit = units
        .find(_.convert(duration.length, duration.unit) > 0)
        .getOrElse(NANOSECONDS)
      val value      = duration.toUnit(chosenUnit)
      "%.2f %s".format(value, abbreviate(chosenUnit))
    case Duration.MinusInf        => "-∞ (minus infinity)"
    case Duration.Inf             => "∞ (infinity)"
    case _                        => "undefined"
  }
