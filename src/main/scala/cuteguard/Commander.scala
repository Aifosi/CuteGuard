package cuteguard

import cuteguard.commands.*
import cuteguard.model.{Discord, DiscordID}
import cuteguard.syntax.action.*

import cats.effect.{Deferred, IO}
import cats.syntax.foldable.*
import org.typelevel.log4cats.Logger

import scala.util.chaining.*

case class Commander[Log <: DiscordLogger](
  logger: Log,
  commands: List[AnyCommand],
)(using Logger[IO]):
  lazy val textCommands: List[TextCommand]                = commands.collect { case command: TextCommand =>
    command
  }
  lazy val reactionCommands: List[ReactionCommand]        = commands.collect { case command: ReactionCommand =>
    command
  }
  lazy val slashCommands: List[SlashCommand]              = commands.collect { case command: SlashCommand =>
    command
  }
  lazy val autoCompletableCommands: List[AutoCompletable] = commands.collect { case command: AutoCompletable =>
    command
  }

  def registerSlashCommands(discordDeferred: Deferred[IO, Discord]): IO[Unit] = for
    discord         <- discordDeferred.get
    slashCommandData = SlashPattern.buildCommands(slashCommands.map(_.pattern))
    _               <- discord.guilds.traverse_ { guild =>
                         for
                           commandsAdded <- guild.addCommands(slashCommandData)
                           commands      <- guild.commands
                           _             <- commands.collect {
                                              case jdaCommand if !commandsAdded.contains(DiscordID(jdaCommand.getIdLong)) =>
                                                jdaCommand.delete.toIO
                                            }.sequence_
                         yield ()
                       }
    _               <- Logger[IO].info("All Slash commands registered.")
  yield ()

  def withDefaults: Commander[Log] =
    val allCommands = commands.pipe(commands => commands :+ new Help(commands))
    copy(commands = allCommands)
