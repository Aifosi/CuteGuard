package cuteguard.model.discord

import cuteguard.syntax.action.*

import cats.effect.IO
import net.dv8tion.jda.api.entities.Member as JDAMember

import scala.compiletime.asMatchable
import scala.jdk.CollectionConverters.*

open class Member(private[model] val member: JDAMember) extends User(member.getUser):
  lazy val nameInGuild: String = Option(member.getEffectiveName).getOrElse(accountName)

  lazy val guild: Guild      = new Guild(member.getGuild)
  lazy val guildName: String = member.getEffectiveName

  def isSelfMuted: Boolean = member.getVoiceState.isSelfMuted

  def isGuildMuted: Boolean = member.getVoiceState.isGuildMuted

  def mute: IO[Unit] = member.mute(true).toIO.void

  def unmute: IO[Unit] = member.mute(false).toIO.void

  def toggleMute: IO[Unit] = member.mute(!isGuildMuted).toIO.void

  def roles: Set[Role] = member.getRoles.asScala.toSet.map(new Role(_))

  def hasRole(role: Role): Boolean = roles.exists(_.discordID == role.discordID)

  def addRole(role: Role): IO[Unit] = member.getGuild.addRoleToMember(member, role.role).toIO.void

  def removeRole(role: Role): IO[Unit] = member.getGuild.removeRoleFromMember(member, role.role).toIO.void

  def isGuildOwner: Boolean = DiscordID(member.getGuild.getOwnerIdLong) == discordID

  override def equals(other: Any): Boolean = other.asMatchable match
    case that: Member => discordID == that.discordID && guild == that.guild
    case that: User   => discordID == that.discordID
    case _            => false

  override def hashCode(): Int =
    val state = Seq(discordID, guild.discordID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
