package cuteguard.commands

import cuteguard.Cooldown
import cuteguard.model.Action
import cuteguard.model.discord.Embed
import cuteguard.model.discord.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex
case class Pleading(cooldown: Cooldown, link: String) extends TextCommand with NoChannelLog:
  override def pattern: Regex = ".*(<a?:\\w*plead\\w*:\\d+>|\uD83E\uDD7A).*".r

  override def matches(event: MessageEvent): Boolean = pattern.matches(stripAccents(event.content.toLowerCase))

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    lazy val embed = Embed(
      s"${event.authorName}, use your words cutie",
      link,
    )
    cooldown.interact(event.author)(Action.Pleading, event.reply(embed).void)

  override val description: String = "Responds when a user says they are not cute"
