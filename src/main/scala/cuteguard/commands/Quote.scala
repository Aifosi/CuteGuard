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
    for
      authorName    <- event.authorNameInGuild
      messageAuthor <- event.guild.fold(IO.pure(event.message.author.accountName))(event.message.authorNameInGuild)
      quoteChannel  <- quoteChannel
      embed          = new EmbedBuilder()
                         .setTitle(s"$messageAuthor said")
                         .setDescription(event.content)
                         .setFooter(s"This message was quoted by $authorName")
                         .setUrl(event.message.jumpUrl)
                         .setColor(Color(150, 255, 120))
                         .build()
      quote         <- quoteChannel.sendEmbed(embed)
      _             <- event.replyEphemeral(s"Message quoted you can find it [here](${quote.jumpUrl})")
    yield true
