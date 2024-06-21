package cuteguard.commands

import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

object Subsmash extends TextCommand with NoLog:
  override def pattern: Regex = "[^ \n]{15,}".r

  override def matches(event: MessageEvent): Boolean =
    val filteredText =
      stripAccents(event.content)    // Remove diacritics
        .toLowerCase                 // to lowercase
        .replaceAll("http[^ ]+", "") // remove links
        .replaceAll("<.+?>", "")     // remove emoji and links
        .replaceAll(":.+?:", "")     // remove more emoji
        .replaceAll("`.+?`(:?``)?", "") // remove code blocks
    val matches = pattern.findFirstIn(filteredText).nonEmpty
    if matches then
      println(s"sender: ${event.authorName}")
      println(s"content: ${event.content}")
      println(s"contentStripped: ${event.contentStripped}")
      println(s"contentDisplay: ${event.contentDisplay}")
      println(s"filteredText: $filteredText")
    matches

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] = {
    val embed = Embed(
      s"${event.authorName}, use your words cutie",
      "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
      "created by a sneaky totally not cute kitty",
    )
    event.reply(embed).as(true)
  }

  override val description: String = "Responds when a user says they are not cute"
