package cuteguard.model.discord.event

import cuteguard.model.discord.{DiscordID, Message}
import cuteguard.model.discord.toLong
import cuteguard.syntax.all.*

import cats.effect.IO
import net.dv8tion.jda.api.entities.{Guild as JDAGuild, Member as JDAMember, User as JDAUser}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

class ReactionEvent(
  val content: String,
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  val messageID: DiscordID,
  jdaGuild: Option[JDAGuild],
) extends Event(jdaChannel, jdaAuthor, jdaMember, jdaGuild):

  def addReaction(emoji: String): IO[Unit] =
    for
      message <- jdaChannel.retrieveMessageById(messageID.toLong).toIO
      _       <- message.addReaction(Emoji.fromFormatted(emoji)).toIO
    yield ()

  lazy val message: IO[Message] =
    channel
      .findMessageByID(messageID)
      .getOrRaise(new Exception(s"Message $messageID not found in channel ${channel.discordID}"))

object ReactionEvent:
  def apply(event: MessageReactionAddEvent): ReactionEvent =
    new ReactionEvent(
      event.getReaction.getEmoji.getAsReactionCode,
      event.getChannel,
      event.getUser,
      Option(event.getMember),
      DiscordID(event.getMessageIdLong),
      Option(event.getGuild),
    )
