package cuteguard.commands

import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

object NotCute extends TextCommand with NoLog:
  override def pattern: Regex = "not\\s+cute".r

  override def matches(event: MessageEvent): Boolean = pattern.matches(stripAccents(event.content).toLowerCase)

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] = {
    val embed = Embed(
      s"Lies - you're cute ${event.authorName}",
      "According to server rule 1, you are cute.\nJust accept it cutie! \uD83D\uDC9C",
      "https://media.tenor.com/iESegr2Kb6MAAAAC/narpy-cute.gif",
      "created by a sneaky totally not cute kitty",
    )
    event.reply(embed).as(true)
  }

  override val description: String = "Responds when a user says they are not cute"
