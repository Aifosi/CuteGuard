package cuteguard.model.discord.event

import cuteguard.model.discord.Message

import cats.effect.IO
import net.dv8tion.jda.api.entities.{
  Guild as JDAGuild,
  Member as JDAMember,
  Message as JDAMessage,
  MessageEmbed,
  User as JDAUser,
}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import scala.util.Try

class MessageEvent(
  jdaMessage: JDAMessage,
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  jdaGuild: Option[JDAGuild],
) extends GenericTextEvent(jdaChannel, jdaAuthor, jdaMember, jdaGuild):
  lazy val message: Message        = new Message(jdaMessage)
  lazy val content: String         = message.content
  lazy val contentStripped: String = message.contentStripped
  lazy val contentDisplay: String  = message.contentDisplay

  override def reply(string: String): IO[Message] = message.reply(string)

  override def reply(embed: MessageEmbed): IO[Message] = message.reply(embed)

object MessageEvent:
  given Conversion[MessageReceivedEvent, MessageEvent] = event =>
    new MessageEvent(
      event.getMessage,
      event.getChannel,
      event.getAuthor,
      Option(event.getMember),
      Try(event.getGuild).toOption,
    )
