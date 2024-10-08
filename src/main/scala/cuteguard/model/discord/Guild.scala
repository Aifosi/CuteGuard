package cuteguard.model.discord

import cuteguard.model.discord.Discord.*
import cuteguard.syntax.action.*
import cuteguard.syntax.io.*
import cuteguard.syntax.task.*
import cuteguard.utils.Maybe

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.either.*
import fs2.Stream
import net.dv8tion.jda.api.entities.{Guild as JDAGuild, User as JDAUser}
import net.dv8tion.jda.api.interactions.commands.Command as JDACommand
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

import scala.compiletime.asMatchable
import scala.jdk.CollectionConverters.*

class Guild(private[model] val guild: JDAGuild):
  lazy val discordID: DiscordID = DiscordID(guild.getIdLong)
  lazy val ownerID: DiscordID   = DiscordID(guild.getOwnerIdLong)

  def isOwner(user: User): Boolean = user.discordID == ownerID

  def isOwner(member: Member): Boolean = member.discordID == ownerID

  def roles: List[Role] = guild.getRoles.asScala.toList.map(new Role(_))

  def members: Stream[IO, Member] = guild.loadMembers().toIO.map(_.asScala.map(new Member(_))).streamedIterable

  def member(user: User): Maybe[Member] =
    actionGetter[JDAUser](user.user, "guild member", guild.retrieveMember, new Member(_))

  def member(userDiscordID: DiscordID): Maybe[Member] =
    actionGetter[Long](userDiscordID.toLong, "guild member", guild.retrieveMemberById, new Member(_))

  def members(userDiscordIDs: Set[DiscordID]): Maybe[List[Member]] =
    EitherT {
      guild
        .retrieveMembersByIds(userDiscordIDs.map(_.toLong).toSeq*)
        .toIO
        .map(_.asScala.toList.map(new Member(_)))
        .attempt
        .map(_.leftMap(_ => new Exception(s"Failed to get members using: $userDiscordIDs")))
    }

  def addCommands(commands: List[SlashCommandData]): IO[List[DiscordID]] =
    guild
      .updateCommands()
      .addCommands(commands*)
      .toIO
      .map(_.asScala.toList.map(command => DiscordID(command.getIdLong)))

  def commands: IO[List[JDACommand]] = guild.retrieveCommands().toIO.map(_.asScala.toList)

  override def equals(other: Any): Boolean = other.asMatchable match
    case that: Guild => discordID == that.discordID
    case _           => false

  override def hashCode(): Int =
    val state = Seq(discordID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
