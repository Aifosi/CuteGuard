package cuteguard.model.discord.event

import cuteguard.mapping.OptionWritter
import cuteguard.syntax.action.*
import cuteguard.syntax.string.*

import cats.effect.IO
import net.dv8tion.jda.api.entities.{Guild as JDAGuild, Member as JDAMember, User as JDAUser}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping

import scala.compiletime.{asMatchable, erasedValue}
import scala.jdk.CollectionConverters.*

class AutoCompleteEvent(
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  jdaGuild: Option[JDAGuild],
  val underlying: CommandAutoCompleteInteractionEvent,
) extends Event(jdaChannel, jdaAuthor, jdaMember, jdaGuild):
  def focusedOption: String        = underlying.getFocusedOption.getName
  def focusedValue: String         = underlying.getFocusedOption.getValue
  def options: List[OptionMapping] = underlying.getOptions.asScala.toList

  def filteredOptions[T](options: List[T])(using writter: OptionWritter[T]): List[T] =
    options.flatMap { option =>
      Option.when(writter(option).startsWithIgnoreCase(focusedValue))(option)
    }

  transparent inline def replyChoices[T: OptionWritter](options: List[T]): IO[Unit] =
    inline erasedValue[T] match
      case _: Int    =>
        println("Int")
        underlying.replyChoiceLongs(filteredOptions(options).asInstanceOf[List[Int]].map(_.toLong)*).toIO.void
      case _: Long   =>
        println("Long")
        underlying.replyChoiceLongs(filteredOptions(options).asInstanceOf[List[Long]]*).toIO.void
      case _: Double =>
        println("Double")
        underlying.replyChoiceDoubles(filteredOptions(options).asInstanceOf[List[Double]]*).toIO.void
      case _         =>
        println("Other")
        val filteredOptions =
          options.map(summon[OptionWritter[T]].apply).filter(_.startsWithIgnoreCase(focusedValue))
        underlying.replyChoiceStrings(filteredOptions*).toIO.void

  lazy val commandName: String                 = underlying.getName
  lazy val subCommandGroupName: Option[String] = Option(underlying.getSubcommandGroup)
  lazy val subCommandName: Option[String]      = Option(underlying.getSubcommandName)
  lazy val fullCommand: String                 = List(Some(commandName), subCommandGroupName, subCommandName).flatten.mkString(" ")

object AutoCompleteEvent:
  def apply(event: CommandAutoCompleteInteractionEvent): AutoCompleteEvent = new AutoCompleteEvent(
    event.getMessageChannel,
    event.getUser,
    Option(event.getMember),
    Option(event.getGuild),
    event,
  )
  given Conversion[CommandAutoCompleteInteractionEvent, AutoCompleteEvent] = AutoCompleteEvent.apply
