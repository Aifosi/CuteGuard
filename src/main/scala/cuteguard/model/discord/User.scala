package cuteguard.model.discord

import cuteguard.mapping.{OptionReader, OptionWritter}
import cuteguard.model.discord.*
import cuteguard.syntax.action.*
import cuteguard.syntax.io.*

import cats.Show
import cats.data.OptionT
import cats.effect.IO
import net.dv8tion.jda.api.entities.User as JDAUser
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.compiletime.asMatchable

open class User(private[model] val user: JDAUser):
  lazy val discordID: DiscordID = user.getIdLong
  lazy val mention: String      = user.getAsMention // s"<@!$discordID>"
  lazy val accountName: String  = user.getName
  lazy val isBot: Boolean       = user.isBot

  lazy val privateChannel: IO[PrivateChannel] = user.openPrivateChannel.toIO.map(new PrivateChannel(_))

  def sendMessage(message: String): IO[Message] = privateChannel.flatMap(_.sendMessage(message))

  private def unsafeMember(guild: Guild): IO[Member] = guild.member(this).rethrowT

  def addRole(guild: Guild, role: Role): IO[Unit] = unsafeMember(guild).flatMap(_.addRole(role))

  def removeRole(guild: Guild, role: Role): IO[Unit] = unsafeMember(guild).flatMap(_.removeRole(role))

  def member(guild: Guild): OptionT[IO, Member] =
    for
      given Logger[IO] <- OptionT.liftF(Slf4jLogger.create[IO])
      member           <- OptionT(unsafeMember(guild).logErrorOption)
    yield member

  def getNameIn(guild: Guild): OptionT[IO, String] = member(guild).map(_.guildName)

  def hasRole(guild: Guild, role: Role): IO[Boolean] = member(guild).exists(_.hasRole(role))

  override lazy val toString: String = s"$accountName($discordID)"

  override def equals(other: Any): Boolean = other.asMatchable match
    case that: User => discordID == that.discordID
    case _          => false

  override def hashCode(): Int =
    val state = Seq(discordID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)

object User:
  given Show[User] = Show.fromToString

  given OptionReader[User]  = OptionReader.shouldNeverBeUsed("User")
  given OptionWritter[User] = OptionWritter.shouldNeverBeUsed("User")
