package cuteguard.commands

import cuteguard.Named
import cuteguard.commands.SlashPattern.*
import cuteguard.model.event.{Event, MessageEvent, ReactionEvent, SlashCommandEvent}

import cats.effect.IO
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

object Command:
  val all = ".+".r

type AnyCommand = Command[?, ? <: Event]

sealed abstract class Command[T, E <: Event] extends Named:
  def pattern: T

  def apply(pattern: T, event: E)(using Logger[IO]): IO[Boolean]

  val description: String

  override def toString: String = className

  def matches(event: E): Boolean

abstract class TextCommand extends Command[Regex, MessageEvent]:
  override def matches(event: MessageEvent): Boolean = pattern.matches(event.content)

object TextCommand:
  val any: Regex         = ".+".r
  val userMention: Regex = "<@!(\\d+)>".r

abstract class ReactionCommand extends Command[String, ReactionEvent]:
  override def matches(event: ReactionEvent): Boolean = pattern == event.content

abstract class SlashCommand extends Command[SlashPattern, SlashCommandEvent]:
  /** If set to false only admins can see it by default.
    */
  val isUserCommand: Boolean

  val fullCommand: String

  final lazy val (command: String, subCommandGroup: Option[String], subCommand: Option[String]) =
    fullCommand.split(" ").toList match {
      case List(command, subCommandGroup, subCommand) => (command, Some(subCommandGroup), Some(subCommand))
      case List(command, subCommand)                  => (command, None, Some(subCommand))
      case List(command)                              => (command, None, None)
      case _                                          => throw new Exception(s"Invalid command $fullCommand")
    }

  final protected lazy val slashPattern: SlashPattern =
    SlashPattern(command, subCommandGroup, subCommand, description, isUserCommand)

  override lazy val pattern: SlashPattern = slashPattern

  override def matches(event: SlashCommandEvent): Boolean =
    event.fullCommand.equalsIgnoreCase(fullCommand)
