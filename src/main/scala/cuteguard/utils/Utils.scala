package cuteguard.utils

import cuteguard.mapping.OptionResult

import cats.data.EitherT
import cats.effect.IO

type Maybe[T] = EitherT[IO, Throwable, T]

extension (string: String)
  def startsWithIgnoreCase(other: String): Boolean = string.toLowerCase.startsWith(other.toLowerCase)

extension [T](result: OptionResult[T]) def toEitherT: EitherT[IO, String, T] = EitherT.fromEither(result)
