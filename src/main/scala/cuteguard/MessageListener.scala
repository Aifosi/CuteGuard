package cuteguard

import cuteguard.commands.{Command, NoChannelLog}
import cuteguard.model.event.{Event, MessageEvent, ReactionEvent, SlashCommandEvent}
import cuteguard.model.event.MessageEvent.given
import cuteguard.model.event.ReactionEvent.given
import cuteguard.model.event.SlashCommandEvent.given
import cuteguard.syntax.io.*

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import net.dv8tion.jda.api.events.interaction.command.{
  CommandAutoCompleteInteractionEvent,
  SlashCommandInteractionEvent,
}
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.typelevel.log4cats.Logger

class MessageListener(
  commander: Commander[?],
)(using l: Logger[IO], r: IORuntime, discordLogger: DiscordLogger)
    extends ListenerAdapter:
  private def runCommandList[T, E <: Event](
    event: E,
    commands: List[Command[T, E]],
  )(
    log: (E, Command[T, E]) => IO[Unit],
  ): IO[Unit] =
    if !event.author.isBot then
      commands
        .sortBy(-_.pattern.toString.length)
        .foldLeft(IO.pure(false)) {
          case (io, command) if command.matches(event) =>
            for
              stopped <- io.logError(true)
              stop    <-
                if stopped then IO.pure(true)
                else
                  for
                    continue <- command.apply(command.pattern, event)
                    _        <- IO.unlessA(continue)(log(event, command))
                  yield continue
            yield stop
          case (io, _)                                 => io
        }
        .void
    else IO.unit

  private def log(event: Event, message: String, ignoreChannel: Boolean): IO[Unit] =
    for
      guild  <- event.guild
      mention = if guild.isOwner(event.author) then event.author.accountName else event.author.mention
      _      <- IO.unlessA(ignoreChannel)(discordLogger.logToChannel(mention + message))
      _      <- Logger[IO].info(event.author.toString + message)
    yield ()

  override def onMessageReceived(event: MessageReceivedEvent): Unit =
    runCommandList(event, commander.textCommands) { (event, command) =>
      lazy val subgroupsText =
        val subgroups = command.pattern.findFirstMatchIn(event.content).fold("")(_.subgroups.mkString(" "))
        if subgroups.isBlank then event.content else subgroups

      if command.pattern != Command.all then
        log(event, s" issued text command $command: $subgroupsText".stripTrailing, command.isInstanceOf[NoChannelLog])
      else IO.unit
    }.unsafeRunAndForget()

  override def onMessageReactionAdd(event: MessageReactionAddEvent): Unit =
    runCommandList(event, commander.reactionCommands) { (event, command) =>
      log(event, s" issued reaction command $command".stripTrailing, command.isInstanceOf[NoChannelLog])
    }.unsafeRunAndForget()

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit =
    runCommandList(event, commander.slashCommands) { (event, command) =>
      val options = event.allOptions.map {
        case option
            if option.getType == OptionType.MENTIONABLE || option.getType == OptionType.USER || option.getType == OptionType.ROLE || option.getType == OptionType.CHANNEL =>
          s"${option.getName}: ${option.getAsMentionable.getAsMention}"
        case option =>
          s"${option.getName}: ${option.getAsString}"
      }.mkString(", ")
      log(
        event,
        s" issued slash command $command${if options.nonEmpty then s", options: $options" else ""}".stripTrailing,
        command.isInstanceOf[NoChannelLog],
      )
    }.unsafeRunAndForget()

  override def onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent): Unit =
    commander.autoCompletableCommands
      .foldLeft(IO.pure(false)) {
        case (io, command) if command.matchesAutoComplete(event) =>
          for
            stopped <- io
            stop    <- if stopped then IO.pure(true) else command.apply(event)
          yield stop
        case (io, _)                                             => io
      }
      .void
      .unsafeRunAndForget()

/* override def onGuildMemberRemove(event: GuildMemberRemoveEvent): Unit =
    val io = for
      logger  <- Slf4jLogger.create[IO]
      message <- registration.unregister(new Member(event.getMember))
      _       <- message.fold(IO.unit)(new User(event.getUser).sendMessage)
    yield ()
    io.unsafeRunSync()*/
