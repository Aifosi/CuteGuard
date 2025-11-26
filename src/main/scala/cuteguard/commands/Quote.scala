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
    quoteChannel.flatMap { quoteChannel =>
      val guild = quoteChannel.guild.get
      if guild != event.message.guild then event.replyEphemeral(s"You can only quote messages from ${guild.name}.")
      else
        for
          authorName    <- event.authorNameInGuild
          messageAuthor <- event.message.authorNameInGuild(guild)
          embed          = new EmbedBuilder()
                             .setTitle(s"$messageAuthor said")
                             .setDescription(event.content)
                             .setFooter(s"This message was quoted by $authorName")
                             .setImage(event.message.attachments.headOption.map(_.getUrl).orNull)
                             .setUrl(event.message.jumpUrl)
                             .setColor(Color(150, 255, 120))
                             .build()
          quote         <- quoteChannel.sendEmbed(embed)
          _             <- event.replyEphemeral(s"Message quoted you can find it [here](${quote.jumpUrl})")
        yield ()

    }.as(true)
