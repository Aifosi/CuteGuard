package cuteguard

import cuteguard.db.Users
import cuteguard.model.discord.{Member, Message}
import cuteguard.model.discord.User as DiscordUser
import cuteguard.syntax.action.*

import cats.data.OptionT
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.hooks.EventListener

class ActivityMonitor(users: Users)(using IORuntime) extends EventListener:
  override def onEvent(event: GenericEvent): Unit =
    val author: OptionT[IO, DiscordUser] = event match
      // All message or reaction send, edit, delete and adding or removing votes from polls
      case event: GenericMessageEvent           =>
        OptionT.liftF(event.getChannel.retrieveMessageById(event.getMessageIdLong).toIO.map(new Message(_).author))
      // All slash commands, buttons or menu interaction
      case event: GenericInteractionCreateEvent =>
        OptionT.pure(new DiscordUser(event.getUser))
      // Joining or leaving voice channels
      case event: GuildVoiceUpdateEvent         =>
        OptionT.pure(new Member(event.getMember))
      // Joining the server
      case event: GuildMemberJoinEvent          =>
        OptionT.pure(new Member(event.getMember))
      case _                                    => OptionT.none

    author
      .filterNot(_.isBot)
      .semiflatMap(users.addActivity(_, Some("Run activity monitor")))
      .value
      .unsafeRunAndForget()

/*def run =
  val allFromDB = activity.all(Some("Run activity monitor"))
  guild.members.evalMap()
 */
