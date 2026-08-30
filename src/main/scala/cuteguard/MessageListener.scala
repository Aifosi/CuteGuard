package cuteguard

import cuteguard.DurationShow.given
import cuteguard.Result.Continue
import cuteguard.commands.{AnyCommand, FunnyCommand, NoChannelLog, PatternCommand, Quote, SlashCommand}
import cuteguard.model.discord.Role
import cuteguard.model.discord.event.{
  AutoCompleteEvent,
  Event,
  InteractionMixin,
  MessageEvent,
  MessageInteractionEvent,
  ReactionEvent,
  SlashCommandEvent,
}
import cuteguard.syntax.io.*

import cats.Show
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.instances.list.*
import cats.syntax.foldable.*
import cats.syntax.show.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.{
  CommandAutoCompleteInteractionEvent,
  MessageContextInteractionEvent,
  SlashCommandInteractionEvent,
}
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*

enum Result:
  case Continue, Stop, Skip

class MessageListener(
  commander: Commander,
  funnyBotRole: IO[Role],
)(using l: Logger[IO], r: IORuntime, discordLogger: DiscordLogger)
    extends ListenerAdapter:
  private def runTimedCommand(
    run: IO[Unit],
  )(
    log: Long => IO[Unit],
  ): IO[Unit] = for
    start <- IO(System.nanoTime)
    _     <- run
    end   <- IO(System.nanoTime)
    _     <- log(end - start)
  yield ()

  private def funnyCheck(event: Event, command: AnyCommand): IO[Result] = command match
    case _: FunnyCommand =>
      funnyBotRole.flatMap { role =>
        val hasOverride: Permission => Boolean =
          (permission: Permission) => event.channel.permissionContainer.exists(_.hasAllowedOverride(role, permission))
        val hasPermission                      = command match
          case command: SlashCommand => hasOverride(Permission.USE_APPLICATION_COMMANDS)
          case command: Quote        => hasOverride(Permission.PIN_MESSAGES)
          case command               => hasOverride(Permission.MESSAGE_SEND)

        if hasPermission then IO.pure(Result.Continue)
        else
          event match
            case interactionEvent: InteractionMixin =>
              interactionEvent.replyEphemeral(s"Cannot use `$command` command in this channel.").as(Result.Skip)
            case _                                  => IO.pure(Result.Skip)
      }
    case _               => IO.pure(Result.Continue)

  private def runCommandList[T, E <: Event](
    event: E,
    commands: List[PatternCommand[T, E]],
  )(
    log: (E, PatternCommand[T, E], Long) => IO[Unit],
  ): IO[Unit] =
    if !event.author.isBot then
      commands
        .sortBy(-_.pattern.toString.length)
        .foldLeft(IO.pure(Result.Continue)) {
          case (io, command) if command.matches(event) =>
            for
              previous   <- io
              funnyCheck <- if previous == Result.Continue then funnyCheck(event, command) else IO.pure(previous)
              result     <- funnyCheck match
                              case Result.Continue =>
                                runTimedCommand(command.apply(command.pattern, event))(log(event, command, _))
                                  .as(Result.Stop)
                              case Result.Skip     =>
                                Logger[IO]
                                  .debug(s"Matched $command but skipping because not in a Funny Channel")
                                  .as(Result.Continue)
                              case Result.Stop     => IO.pure(Result.Stop)
            yield result

          case (io, _) => io
        }
        .logError(Result.Stop)
        .void
    else IO.unit

  private def log(event: Event, message: String, ignoreChannel: Boolean, nanos: Long): IO[Unit] =
    val mention = if event.guild.exists(_.isOwner(event.author)) then event.author.accountName else event.author.mention
    for
      _   <- IO.unlessA(ignoreChannel)(discordLogger.logToChannel(mention + message))
      time = show", command took ${nanos.nanos} to run."
      _   <- Logger[IO].info(event.author.toString + message + time)
    yield ()

  override def onMessageReceived(event: MessageReceivedEvent): Unit =
    runCommandList(MessageEvent(event), commander.textCommands) { (event, command, nanos) =>
      lazy val subgroupsText =
        val subgroups = command.pattern.findFirstMatchIn(event.content).fold("")(_.subgroups.mkString(" "))
        if subgroups.isBlank then event.content else subgroups

      if command.pattern != PatternCommand.all then
        log(
          event,
          s" issued text command $command: $subgroupsText".stripTrailing,
          command.isInstanceOf[NoChannelLog],
          nanos,
        )
      else IO.unit
    }.unsafeRunAndForget()

  override def onMessageReactionAdd(event: MessageReactionAddEvent): Unit =
    runCommandList(ReactionEvent(event), commander.reactionCommands) { (event, command, nanos) =>
      log(event, s" issued reaction command $command".stripTrailing, command.isInstanceOf[NoChannelLog], nanos)
    }.unsafeRunAndForget()

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit =
    runCommandList(SlashCommandEvent(event), commander.slashCommands) { (event, command, nanos) =>
      val options = event.allOptions.map {
        case option
            if option.getType == OptionType.MENTIONABLE || option.getType == OptionType.USER || option.getType ==
              OptionType.ROLE || option.getType == OptionType.CHANNEL =>
          s"${option.getName}: ${option.getAsMentionable.getAsMention}"
        case option =>
          s"${option.getName}: ${option.getAsString}"
      }.mkString(", ")
      log(
        event,
        s" issued slash command $command${if options.nonEmpty then s", options: [$options]" else ""}".stripTrailing,
        command.isInstanceOf[NoChannelLog],
        nanos,
      )
    }.unsafeRunAndForget()

  override def onCommandAutoCompleteInteraction(jdaEvent: CommandAutoCompleteInteractionEvent): Unit =
    val event = AutoCompleteEvent(jdaEvent)
    commander.autoCompletableCommands.collect {
      case command if command.matchesAutoComplete(event) => command.apply(event)
    }.sequence_.unsafeRunAndForget()

  override def onMessageContextInteraction(jdaEvent: MessageContextInteractionEvent): Unit =
    val event = MessageInteractionEvent(jdaEvent)
    commander.messageInteractionCommands.collect {
      case command if command.matches(event) =>
        funnyCheck(event, command).flatMap {
          case Continue =>
            runTimedCommand(command.apply(event)) { duration =>
              log(
                event,
                s" used message interaction command $command",
                command.isInstanceOf[NoChannelLog],
                duration,
              )
            }
          case _        => IO.unit
        }
    }.sequence_.unsafeRunAndForget()

/* override def onGuildMemberRemove(event: GuildMemberRemoveEvent): Unit =
    val io = for
      logger  <- Slf4jLogger.create[IO]
      message <- registration.unregister(new Member(event.getMember))
      _       <- message.fold(IO.unit)(new User(event.getUser).sendMessage)
    yield ()
    io.unsafeRunSync()*/
