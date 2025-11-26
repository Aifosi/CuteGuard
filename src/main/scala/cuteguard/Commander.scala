package cuteguard

import cuteguard.commands.*
import cuteguard.model.discord.{Discord, DiscordID}
import cuteguard.syntax.action.*

import cats.effect.{Deferred, IO}
import cats.syntax.foldable.*
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.typelevel.log4cats.Logger

class Commander private (
  commands: List[AnyCommand],
)(using Logger[IO]):
  lazy val textCommands: List[TextCommand]                             = commands.collect { case command: TextCommand =>
    command
  }
  lazy val reactionCommands: List[ReactionCommand]                     = commands.collect { case command: ReactionCommand =>
    command
  }
  lazy val slashCommands: List[SlashCommand]                           = commands.collect { case command: SlashCommand =>
    command
  }
  lazy val autoCompletableCommands: List[AutoCompletable[?]]           = commands.collect { case command: AutoCompletable[?] =>
    command
  }
  lazy val messageInteractionCommands: List[MessageInteractionCommand] = commands.collect {
    case command: MessageInteractionCommand =>
      command
  }

  def registerSlashCommands(discordDeferred: Deferred[IO, Discord]): IO[Unit] = for
    discord                       <- discordDeferred.get
    slashCommandData               = SlashPattern.buildCommands(slashCommands.map(_.pattern))
    messageInteractionCommandsData = messageInteractionCommands.map(command => Commands.message(command.name))
    _                             <- discord.guilds.traverse_ { guild =>
                                       for
                                         commandsAdded <- guild.addCommands(slashCommandData ++ messageInteractionCommandsData)
                                         commands      <- guild.commands
                                         _             <- commands.collect {
                                                            case jdaCommand if !commandsAdded.contains(DiscordID(jdaCommand.getIdLong)) =>
                                                              jdaCommand.delete.toIO
                                                          }.sequence_
                                       yield ()
                                     }
    _                             <- Logger[IO].info("All commands registered.")
  yield ()

object Commander:
  def apply(commands: List[AnyCommand])(using Logger[IO]): Commander = new Commander(commands :+ new Help(commands))
