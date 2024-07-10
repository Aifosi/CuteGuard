package cuteguard.commands

import cuteguard.Cooldown
import cuteguard.model.discord.Embed
import cuteguard.model.discord.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex
case class Pleading(cooldown: Cooldown) extends TextCommand with NoChannelLog:
  override def pattern: Regex = ".*(<a?:\\w*pleading\\w*:\\d+>|\uD83E\uDD7A).*".r

  override def matches(event: MessageEvent): Boolean = pattern.matches(stripAccents(event.content.toLowerCase))

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    lazy val embed = Embed(
      s"${event.authorName}, use your words cutie",
      "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
      "created by a sneaky totally not cute kitty",
    )
    cooldown.interact(event.author)(event.reply(embed).void)

  override val description: String = "Responds when a user says they are not cute"
