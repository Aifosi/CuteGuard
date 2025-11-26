package cuteguard.model.discord.event

import cuteguard.commands.MacroHelper
import cuteguard.mapping.{OptionReader, OptionResult}
import cuteguard.model.discord.Message
import cuteguard.syntax.action.*

import cats.effect.IO
import cats.syntax.option.*
import javax.imageio.ImageIO
import net.dv8tion.jda.api.entities.{Guild as JDAGuild, Member as JDAMember, User as JDAUser}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook as JDAInteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import scala.jdk.CollectionConverters.*

trait SlashAPI:
  def reply(string: String): IO[Message]
  def replyEphemeral(string: String): IO[Option[Message]]
  def replyImage(image: BufferedImage, title: String, ephemeral: Boolean = false): IO[Option[Message]]

class InteractionHook(
  underlying: JDAInteractionHook,
) extends SlashAPI:
  override def reply(string: String): IO[Message] =
    underlying.sendMessage(string).toIO.map(Message(_))

  override def replyEphemeral(string: String): IO[Option[Message]] =
    underlying.sendMessage(string).toIO.map(Message(_).some)

  override def replyImage(image: BufferedImage, title: String, ephemeral: Boolean = false): IO[Option[Message]] =
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    underlying.sendFiles(FileUpload.fromData(outputStream.toByteArray, title + ".png")).toIO.map(Message(_).some)

class SlashCommandEvent(
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  jdaGuild: Option[JDAGuild],
  underlying: SlashCommandInteractionEvent,
) extends GenericTextEvent(jdaChannel, jdaAuthor, jdaMember, jdaGuild) with SlashAPI:
  def deferReply(ephemeral: Boolean = false): IO[Unit] = underlying.deferReply(ephemeral).toIO.void

  override def reply(string: String): IO[Message] =
    underlying.reply(string).toIO.flatMap(_.retrieveOriginal.toIO).map(Message(_))

  override def replyEphemeral(string: String): IO[Option[Message]] =
    underlying.reply(string).setEphemeral(true).toIO.as(None)

  override def replyImage(image: BufferedImage, title: String, ephemeral: Boolean = false): IO[Option[Message]] =
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    underlying
      .reply(title)
      .addFiles(FileUpload.fromData(outputStream.toByteArray, title + ".png"))
      .setEphemeral(ephemeral)
      .toIO
      .flatMap {
        case _ if ephemeral  => IO.pure(None)
        case interactionHook => interactionHook.retrieveOriginal.toIO.map(Message(_).some)
      }

  inline def getOption[T: OptionReader as reader](option: String): OptionResult[T] =
    MacroHelper.getOption[T](reader, underlying, option)

  lazy val allOptions: List[OptionMapping]     = underlying.getOptions.asScala.toList
  lazy val commandName: String                 = underlying.getName
  lazy val subCommandGroupName: Option[String] = Option(underlying.getSubcommandGroup)
  lazy val subCommandName: Option[String]      = Option(underlying.getSubcommandName)
  lazy val fullCommand: String                 = List(Some(commandName), subCommandGroupName, subCommandName).flatten
    .mkString(" ")
  lazy val hook: InteractionHook               = new InteractionHook(underlying.getHook)

object SlashCommandEvent:
  def apply(event: SlashCommandInteractionEvent): SlashCommandEvent =
    new SlashCommandEvent(
      event.getChannel,
      event.getUser,
      Option(event.getMember),
      Option(event.getGuild),
      event,
    )
