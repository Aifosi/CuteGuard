package cuteguard.syntax

import cuteguard.syntax.stream.*

import cats.effect.IO
import cats.syntax.applicative.*
import cats.syntax.option.*
import fs2.Stream
import org.typelevel.log4cats.Logger

trait IOSyntax:
  extension [A](io: IO[IterableOnce[A]]) def streamedIterable: Stream[IO, A] = Stream.evalI(io)

  extension [A](io: IO[A])
    def streamed: Stream[IO, A]                          = Stream.eval(io)
    def logError(default: => A)(using Logger[IO]): IO[A] =
      io.attempt.flatMap(_.fold(error => Logger[IO].error(error)(error.getMessage).as(default), _.pure))
    def logErrorOption(using Logger[IO]): IO[Option[A]]  =
      io.attempt.flatMap(_.fold(error => Logger[IO].error(error)(error.getMessage).as(None), _.some.pure))
