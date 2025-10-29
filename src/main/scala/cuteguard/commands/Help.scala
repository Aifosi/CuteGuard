package cuteguard.commands

import cuteguard.model.discord.event.SlashCommandEvent

import cats.effect.IO
import org.typelevel.log4cats.Logger

class Help(commands: List[AnyCommand]) extends SlashCommand:
  override val fullCommand: String = "help"
  override val description: String = "Shows help for existing commands"

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val commandWithDescriptions = commands.filter {
      case _: Hidden             => false
      case command: SlashCommand => command.isUserCommand
      case _                     => true
    }.map(command =>
      s"**${if command.isInstanceOf[SlashCommand] then "/" else ""}${command.pattern}** - ${command.description}",
    )
    event.replyEphemeral(commandWithDescriptions.mkString("\n")).as(true)
