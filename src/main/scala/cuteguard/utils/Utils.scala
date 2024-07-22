package cuteguard.utils

import cats.data.EitherT
import cats.effect.IO

type Maybe[T] = EitherT[IO, Throwable, T]

extension (string: String)
  def startsWithIgnoreCase(other: String): Boolean = string.toLowerCase.startsWith(other.toLowerCase)
