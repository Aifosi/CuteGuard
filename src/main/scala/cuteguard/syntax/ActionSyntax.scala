package cuteguard.syntax

import cats.effect.IO
import net.dv8tion.jda.api.requests.RestAction

trait ActionSyntax:
  extension [A](action: RestAction[A])
    def toIO: IO[A] = IO.async_[A] { cb =>
      action.queue(
        result => cb(Right(result)),
        error => cb(Left(error)),
      )
    }
