package cuteguard.model.discord

import cuteguard.mapping.{OptionReader, OptionWriter}
import cuteguard.syntax.action.*
import cuteguard.syntax.io.*

import cats.Show
import cats.data.OptionT
import cats.effect.IO
import net.dv8tion.jda.api.entities.{MessageEmbed, MessageHistory}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.FileUpload
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.File
import scala.compiletime.asMatchable
import scala.jdk.CollectionConverters.*

class Channel(channel: MessageChannel):
  lazy val discordID: DiscordID                                   = DiscordID(channel.getIdLong)
  lazy val mention: String                                        = channel.getAsMention
  lazy val name: String                                           = channel.getName
  def sendMessage(string: String): IO[Message]                    =
    channel.sendMessage(string).toIO.map(new Message(_))
  def sendEmbed(embed: MessageEmbed): IO[Message]                 = channel.sendMessageEmbeds(embed).toIO.map(new Message(_))
  def sendFile(file: File): IO[Message]                           = channel
    .sendFiles(FileUpload.fromData(file))
    .toIO
    .map(new Message(_))
  def findMessageByID(discordID: DiscordID): OptionT[IO, Message] =
    for
      given Logger[IO] <- OptionT.liftF(Slf4jLogger.create[IO])
      message          <- OptionT(channel.retrieveMessageById(discordID.toLong).toIO.logErrorOption).map(new Message(_))
    yield message

  private val history: MessageHistory  = channel.getHistory
  val lastHundred: IO[List[Message]]   =
    history.retrievePast(100).toIO.map(_.asScala.toList.reverse.map(new Message(_)))
  val lastMessage: IO[Option[Message]] = history.retrievePast(1).toIO.map(_.asScala.headOption.map(new Message(_)))

  override def toString: String = mention

  override def equals(other: Any): Boolean = other.asMatchable match
    case that: Channel => discordID == that.discordID
    case _             => false

  override def hashCode(): Int =
    val state = Seq(discordID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)

object Channel:
  given Show[Channel] = Show.fromToString

  given OptionReader[Channel] = OptionReader.shouldNeverBeUsed("Channel")
  given OptionWriter[Channel] = OptionWriter.shouldNeverBeUsed("Channel")
