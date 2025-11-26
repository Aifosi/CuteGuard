package cuteguard.commands

import cuteguard.model.discord.Channel
import cuteguard.model.discord.event.MessageInteractionEvent

import cats.effect.IO
import net.dv8tion.jda.api.EmbedBuilder
import org.typelevel.log4cats.Logger

import java.awt.Color

class Quote(quoteChannel: IO[Channel]) extends MessageInteractionCommand:
  override val name: String = "Quote message"

  override def apply(event: MessageInteractionEvent)(using Logger[IO]): IO[Boolean] =
    quoteChannel.flatMap {
      _.sendEmbed {
        new EmbedBuilder()
          .setTitle(s"${event.authorName} said")
          .setDescription(event.content)
          .setUrl(event.message.jumpUrl)
          .setColor(Color(150, 255, 120))
          .build()
      }
    }.as(true)
