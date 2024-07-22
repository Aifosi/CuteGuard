package cuteguard.syntax

import cats.Applicative
import cats.data.EitherT

trait EitherTSyntax:
  extension (eitherT: EitherT.type)
    def leftWhen[F[_]: Applicative](test: Boolean, error: => String) = EitherT.cond[F](!test, (), error)
