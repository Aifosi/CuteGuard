package cuteguard.model.discord.event

import cuteguard.model.discord.Message
import cuteguard.syntax.action.*

import cats.effect.IO
import cats.syntax.option.*
import javax.imageio.ImageIO
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

trait InteractionMixin(event: GenericCommandInteractionEvent) extends SlashAPI:
  this: Event =>
  def deferReply(ephemeral: Boolean = false): IO[Unit] = event.deferReply(ephemeral).toIO.void

  final override def reply(string: String): IO[Message] =
    event.reply(string).toIO.flatMap(_.retrieveOriginal.toIO).map(Message(_))

  final override def replyEphemeral(string: String): IO[Option[Message]] =
    event.reply(string).setEphemeral(true).toIO.as(None)

  final override def replyImage(image: BufferedImage, title: String, ephemeral: Boolean = false): IO[Option[Message]] =
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    event
      .reply(title)
      .addFiles(FileUpload.fromData(outputStream.toByteArray, title + ".png"))
      .setEphemeral(ephemeral)
      .toIO
      .flatMap {
        case _ if ephemeral  => IO.pure(None)
        case interactionHook => interactionHook.retrieveOriginal.toIO.map(Message(_).some)
      }
