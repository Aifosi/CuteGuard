package cuteguard.commands

import cuteguard.Named
import cuteguard.model.discord.event.{
  Event,
  InteractionEvent,
  MessageEvent,
  MessageInteractionEvent,
  ReactionEvent,
  SlashCommandEvent,
}

import cats.data.EitherT
import cats.effect.IO
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

object PatternCommand:
  val all = ".+".r

type AnyCommand        = Command[? <: Event]
type AnyPatternCommand = PatternCommand[?, ? <: Event]

sealed abstract class Command[E <: Event] extends Named:
  override def toString: String = className

  def matches(event: E): Boolean

sealed abstract class PatternCommand[T, E <: Event] extends Command[E]:
  def pattern: T

  def apply(pattern: T, event: E)(using Logger[IO]): IO[Boolean]

  val description: String

abstract class TextCommand extends PatternCommand[Regex, MessageEvent]:
  override def matches(event: MessageEvent): Boolean = pattern.matches(event.content)

object TextCommand:
  val any: Regex         = ".+".r
  val userMention: Regex = "<@!(\\d+)>".r

abstract class ReactionCommand extends PatternCommand[String, ReactionEvent]:
  override def matches(event: ReactionEvent): Boolean = pattern == event.content

abstract class SlashCommand extends PatternCommand[SlashPattern, SlashCommandEvent]:
  /** If set to false only admins can see it by default.
    */
  val isUserCommand: Boolean = true

  val fullCommand: String

  protected def slashPattern: SlashPattern =
    val (command: String, subCommandGroup: Option[String], subCommand: Option[String]) =
      fullCommand.split(" ").toList match {
        case List(command, subCommandGroup, subCommand) => (command, Some(subCommandGroup), Some(subCommand))
        case List(command, subCommand)                  => (command, None, Some(subCommand))
        case List(command)                              => (command, None, None)
        case _                                          => throw new Exception(s"Invalid command $fullCommand")
      }
    SlashPattern(command, subCommandGroup, subCommand, description, isUserCommand)

  override def pattern: SlashPattern = slashPattern

  override def matches(event: SlashCommandEvent): Boolean =
    event.fullCommand.equalsIgnoreCase(fullCommand)

trait ErrorMessages:
  this: SlashCommand =>

  def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean]

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    run(pattern, event).leftSemiflatMap(event.replyEphemeral(_).as(true)).merge

sealed abstract class InteractionCommand[E <: InteractionEvent] extends Command[E]:
  def name: String

  override def matches(event: E): Boolean = event.name.equalsIgnoreCase(name)

  def apply(event: E)(using Logger[IO]): IO[Boolean]

abstract class MessageInteractionCommand extends InteractionCommand[MessageInteractionEvent]
