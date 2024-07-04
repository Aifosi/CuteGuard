package cuteguard.db

import cats.effect.IO
import doobie.util.log.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object DoobieLogHandler:
  private def createLogHandler(using Logger[IO]): LogHandler[IO] = (logEvent: LogEvent) =>
    logEvent match
      case Success(s, a, l, e1, e2) =>
        Logger[IO]
          .debug(s"""Successful Statement Execution:
            |
            |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.mkString(", ")}]
            | label     = $l
            |   elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis.toString} ms processing (${(e1 + e2).toMillis.toString} ms total)
            """.stripMargin)

      case ProcessingFailure(s, a, l, e1, e2, t) =>
        Logger[IO]
          .error(s"""Failed Resultset Processing:
              |
              |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
              |
              | arguments = [${a.mkString(", ")}]
              | label     = $l
              |   elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis.toString} ms processing (failed) (${(e1 + e2).toMillis.toString} ms total)
              |   failure = ${t.getMessage}
              """.stripMargin)

      case ExecFailure(s, a, l, e1, t) =>
        Logger[IO]
          .error(s"""Failed Statement Execution:
            |
            |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.mkString(", ")}]
            | label     = $l
            |   elapsed = ${e1.toMillis.toString} ms exec (failed)
            |   failure = ${t.getMessage}
            """.stripMargin)

  def create: IO[LogHandler[IO]] =
    for given Logger[IO] <- Slf4jLogger.create[IO]
    yield createLogHandler
