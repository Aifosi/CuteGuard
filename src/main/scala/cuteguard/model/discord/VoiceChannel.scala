package cuteguard.model.discord

import cats.effect.IO
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel as JDAVoiceChannel

import scala.jdk.CollectionConverters.*

class VoiceChannel(channel: JDAVoiceChannel):
  def members: List[Member] = channel.getMembers.asScala.toList.map(new Member(_))

  def toggleMuteAll: List[IO[Unit]] = members.map(_.toggleMute)
  def muteAll: List[IO[Unit]]       = members.map(_.mute)
  def unmuteAll: List[IO[Unit]]     = members.map(_.unmute)
