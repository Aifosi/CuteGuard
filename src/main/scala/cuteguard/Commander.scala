package cuteguard

import cuteguard.commands.*
import cuteguard.model.{Discord, DiscordID}
import cuteguard.syntax.action.*

import cats.effect.IO
import cats.syntax.foldable.*
import org.typelevel.log4cats.Logger

import scala.util.chaining.*

case class Commander[Log <: DiscordLogger](
  logger: Log,
  commands: List[AnyCommand],
  onDiscordAcquired: Discord => IO[Unit],
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

  def registerSlashCommands(discord: Discord): IO[Unit] =
    lazy val slashCommandData = SlashPattern.buildCommands(slashCommands.map(_.pattern))
    discord.guilds.traverse_ { guild =>
      for
        commandsAdded <- guild.addCommands(slashCommandData)
        commands      <- guild.commands
        _             <- commands.collect {
                           case jdaCommand if !commandsAdded.contains(DiscordID(jdaCommand.getIdLong)) => jdaCommand.delete.toIO
                         }.sequence_
      yield ()
    } *> Logger[IO].info("All Slash commands registered.")

  def withDefaults: Commander[Log] =
    val allCommands = commands.pipe(commands => commands :+ new Help(commands))
    copy(commands = allCommands)
