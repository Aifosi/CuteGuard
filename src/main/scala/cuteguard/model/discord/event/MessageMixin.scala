package cuteguard.model.discord.event

import cuteguard.model.discord.Message

import cats.effect.IO
import net.dv8tion.jda.api.entities.{Message as JDAMessage, MessageEmbed}

trait MessageMixin(jdaMessage: JDAMessage):
  this: Event =>
  lazy val message: Message        = new Message(jdaMessage)
  lazy val content: String         = message.content
  lazy val contentStripped: String = message.contentStripped
  lazy val contentDisplay: String  = message.contentDisplay

  override def reply(string: String): IO[Message] = message.reply(string)

  override def reply(embed: MessageEmbed): IO[Message] = message.reply(embed)
