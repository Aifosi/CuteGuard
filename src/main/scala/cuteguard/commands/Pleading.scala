package cuteguard.commands

import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex
object Pleading extends TextCommand with NoLog:
  override def pattern: Regex = ".*(<a?:\\w*pleading\\w*:\\d+>|\uD83E\uDD7A).*".r

  override def matches(event: MessageEvent): Boolean = pattern.matches(stripAccents(event.content.toLowerCase))

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    val embed = Embed(
      s"${event.authorName}, use your words cutie",
      "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
      "created by a sneaky totally not cute kitty",
    )
    event.reply(embed).as(true)

  override val description: String = "Responds when a user says they are not cute"
