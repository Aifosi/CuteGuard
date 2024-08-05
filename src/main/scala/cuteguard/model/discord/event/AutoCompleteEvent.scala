package cuteguard.model.discord.event

import cuteguard.mapping.OptionWriter
import cuteguard.syntax.action.*

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

  transparent inline def replyChoices[T: OptionWriter](options: List[T]): IO[Unit] =
    inline erasedValue[T] match
      case _: Int    =>
        underlying.replyChoiceLongs(options.asInstanceOf[List[Int]].map(_.toLong)*).toIO.void
      case _: Long   =>
        underlying.replyChoiceLongs(options.asInstanceOf[List[Long]]*).toIO.void
      case _: Double =>
        underlying.replyChoiceDoubles(options.asInstanceOf[List[Double]]*).toIO.void
      case _         =>
        underlying.replyChoiceStrings(options.map(summon[OptionWriter[T]].apply)*).toIO.void

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
