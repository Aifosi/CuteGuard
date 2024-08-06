package cuteguard.model.discord

import cuteguard.syntax.action.*

import cats.effect.IO
import cats.instances.list.*
import cats.syntax.foldable.*
import net.dv8tion.jda.api.entities.{Message as JDAMessage, MessageEmbed}
import net.dv8tion.jda.api.entities.emoji.Emoji

class Message(private[model] val message: JDAMessage):
  lazy val content: String         = message.getContentRaw
  lazy val contentStripped: String = message.getContentStripped
  lazy val contentDisplay: String  = message.getContentDisplay
  lazy val id: DiscordID           = DiscordID(message.getIdLong)
  lazy val jumpUrl: String         = message.getJumpUrl

  def addReactions(reactions: String*): IO[Unit]                 =
    reactions.toList.map(Emoji.fromFormatted).traverse_(reaction => message.addReaction(reaction).toIO)
  def addReaction(reaction: String): IO[Unit]                    = message.addReaction(Emoji.fromFormatted(reaction)).toIO.void
  def removeUserReaction(reaction: String, user: User): IO[Unit] =
    message.removeReaction(Emoji.fromFormatted(reaction), user.user).toIO.void
  def edit(string: String): IO[Message]                          = message.editMessage(string).toIO.map(new Message(_))
  def delete: IO[Unit]                                           = message.delete().toIO.void

  def reply(string: String): IO[Message]      = message.reply(string).toIO.map(new Message(_))
  def reply(embed: MessageEmbed): IO[Message] = message.replyEmbeds(embed).toIO.map(new Message(_))

  override def toString: _root_.java.lang.String = s"Message($id, $content)"
