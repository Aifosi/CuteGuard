package cuteguard.model.discord.event

import net.dv8tion.jda.api.entities.{Guild as JDAGuild, Member as JDAMember, Message as JDAMessage, User as JDAUser}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent

import scala.util.Try

class MessageInteractionEvent(
  name: String,
  jdaMessage: JDAMessage,
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  jdaGuild: Option[JDAGuild],
  interactionEvent: MessageContextInteractionEvent,
) extends InteractionEvent(name, jdaChannel, jdaAuthor, jdaMember, jdaGuild) with MessageMixin(jdaMessage)
    with InteractionMixin(interactionEvent)

object MessageInteractionEvent:
  def apply(event: MessageContextInteractionEvent): MessageInteractionEvent =
    new MessageInteractionEvent(
      event.getName,
      event.getTarget,
      event.getChannel,
      event.getUser,
      Option(event.getMember),
      Try(event.getGuild).toOption,
      event,
    )
