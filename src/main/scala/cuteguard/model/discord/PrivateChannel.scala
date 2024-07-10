package cuteguard.model.discord

import cuteguard.syntax.action.*

import cats.effect.IO
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel as JDAPrivateChannel

class PrivateChannel(privateChannel: JDAPrivateChannel):
  def sendMessage(message: String): IO[Message] = privateChannel.sendMessage(message).toIO.map(new Message(_))
