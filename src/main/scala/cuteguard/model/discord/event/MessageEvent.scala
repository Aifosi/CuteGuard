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

open class MessageEvent(
  jdaMessage: JDAMessage,
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  jdaGuild: Option[JDAGuild],
) extends GenericTextEvent(jdaChannel, jdaAuthor, jdaMember, jdaGuild) with MessageMixin(jdaMessage):
  override def reply(string: String): IO[Message] = message.reply(string)

  override def reply(embed: MessageEmbed): IO[Message] = message.reply(embed)

object MessageEvent:
  def apply(event: MessageReceivedEvent): MessageEvent =
    new MessageEvent(
      event.getMessage,
      event.getChannel,
      event.getAuthor,
      Option(event.getMember),
      Try(event.getGuild).toOption,
    )
