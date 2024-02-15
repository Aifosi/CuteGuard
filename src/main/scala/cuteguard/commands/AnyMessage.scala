package cuteguard.commands

import cuteguard.Points
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

class AnyMessage(points: Points, pointsPerMessage: Int) extends TextCommand with Hidden with NoLog:
  override val pattern: Regex = ".*".r

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    for
      member <- event.authorMember
      _      <- points.handleMember(member, event.channel, pointsPerMessage)
    yield true
