package cuteguard.commands

import cuteguard.mapping.{OptionReader, OptionResult}
import cuteguard.model.discord.{Channel, Role, User}
import cuteguard.syntax.action.*

import cats.Show
import cats.effect.IO
import cats.instances.option.*
import cats.syntax.either.*
import cats.syntax.show.*
import cats.syntax.traverse.*
import net.dv8tion.jda.api.events.interaction.command.{
  CommandAutoCompleteInteractionEvent,
  SlashCommandInteractionEvent,
}
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.{SlashCommandData, SubcommandData}

import scala.quoted.*

object MacroHelper:

  private def partial(
    optionType: OptionType,
    required: Boolean,
  )(data: SlashCommandData, name: String, description: String, autoComplete: Boolean): SlashCommandData =
    data.addOption(optionType, name, description, required, autoComplete)

  private def matchT[T: Type](using Quotes): Expr[(SlashCommandData, String, String, Boolean) => SlashCommandData] =
    Type.of[T] match {
      case '[Option[Int]]     => '{ partial(OptionType.INTEGER, false) }
      case '[Option[Long]]    => '{ partial(OptionType.INTEGER, false) }
      case '[Option[Double]]  => '{ partial(OptionType.NUMBER, false) }
      case '[Option[String]]  => '{ partial(OptionType.STRING, false) }
      case '[Option[Boolean]] => '{ partial(OptionType.BOOLEAN, false) }
      case '[Option[User]]    => '{ partial(OptionType.USER, false) }
      case '[Option[Role]]    => '{ partial(OptionType.ROLE, false) }
      case '[Option[Channel]] => '{ partial(OptionType.CHANNEL, false) }

      case '[Int]     => '{ partial(OptionType.INTEGER, true) }
      case '[Long]    => '{ partial(OptionType.INTEGER, true) }
      case '[Double]  => '{ partial(OptionType.NUMBER, true) }
      case '[String]  => '{ partial(OptionType.STRING, true) }
      case '[Boolean] => '{ partial(OptionType.BOOLEAN, true) }
      case '[User]    => '{ partial(OptionType.USER, true) }
      case '[Role]    => '{ partial(OptionType.ROLE, true) }
      case '[Channel] => '{ partial(OptionType.CHANNEL, true) }

      case '[Option[?]] => '{ partial(OptionType.STRING, false) }
      case _            => '{ partial(OptionType.STRING, true) }
    }

  inline def addOption[T] = ${ matchT[T] }

  private def subCommandPartial(
    optionType: OptionType,
    required: Boolean,
  )(data: SubcommandData, name: String, description: String, autoComplete: Boolean): SubcommandData =
    data.addOption(optionType, name, description, required, autoComplete)

  private def subCommandMatchT[T: Type](using
    Quotes,
  ): Expr[(SubcommandData, String, String, Boolean) => SubcommandData] =
    Type.of[T] match {
      case '[Option[Int]]     => '{ subCommandPartial(OptionType.INTEGER, false) }
      case '[Option[Long]]    => '{ subCommandPartial(OptionType.INTEGER, false) }
      case '[Option[Double]]  => '{ subCommandPartial(OptionType.NUMBER, false) }
      case '[Option[String]]  => '{ subCommandPartial(OptionType.STRING, false) }
      case '[Option[Boolean]] => '{ subCommandPartial(OptionType.BOOLEAN, false) }
      case '[Option[User]]    => '{ subCommandPartial(OptionType.USER, false) }
      case '[Option[Role]]    => '{ subCommandPartial(OptionType.ROLE, false) }
      case '[Option[Channel]] => '{ subCommandPartial(OptionType.CHANNEL, false) }

      case '[Int]     => '{ subCommandPartial(OptionType.INTEGER, true) }
      case '[Long]    => '{ subCommandPartial(OptionType.INTEGER, true) }
      case '[Double]  => '{ subCommandPartial(OptionType.NUMBER, true) }
      case '[String]  => '{ subCommandPartial(OptionType.STRING, true) }
      case '[Boolean] => '{ subCommandPartial(OptionType.BOOLEAN, true) }
      case '[User]    => '{ subCommandPartial(OptionType.USER, true) }
      case '[Role]    => '{ subCommandPartial(OptionType.ROLE, true) }
      case '[Channel] => '{ subCommandPartial(OptionType.CHANNEL, true) }

      case '[Option[?]] => '{ subCommandPartial(OptionType.STRING, false) }
      case _            => '{ subCommandPartial(OptionType.STRING, true) }
    }

  inline def addSubCommandOption[T] = ${ subCommandMatchT[T] }

  private def fetchOption[T: Type](using
    Quotes,
  ): Expr[(OptionReader[T], SlashCommandInteractionEvent, String) => OptionResult[T]] =
    Type.of[T] match {
      case '[Option[Int]]     =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(_.getAsLong.toInt).asInstanceOf[T].asRight
        }
      case '[Option[Long]]    =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(_.getAsLong).asInstanceOf[T].asRight
        }
      case '[Option[Double]]  =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(_.getAsDouble).asInstanceOf[T].asRight
        }
      case '[Option[String]]  =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(_.getAsString).asInstanceOf[T].asRight
        }
      case '[Option[Boolean]] =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(_.getAsBoolean).asInstanceOf[T].asRight
        }
      case '[Option[User]]    =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(mapping => new User(mapping.getAsUser)).asInstanceOf[T].asRight
        }
      case '[Option[Role]]    =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option)).map(mapping => new Role(mapping.getAsRole)).asInstanceOf[T].asRight
        }
      case '[Option[Channel]] =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          Option(event.getOption(option))
            .map(mapping => new Channel(mapping.getAsChannel.asGuildMessageChannel))
            .asInstanceOf[T]
            .asRight
        }
      case '[Int]             =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          event.getOption(option).getAsLong.toInt.asInstanceOf[T].asRight
        }
      case '[Long]            =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          event.getOption(option).getAsLong.asInstanceOf[T].asRight
        }
      case '[Double]          =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          event.getOption(option).getAsDouble.asInstanceOf[T].asRight
        }
      case '[String]          =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          event.getOption(option).getAsString.asInstanceOf[T].asRight
        }
      case '[Boolean]         =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          event.getOption(option).getAsBoolean.asInstanceOf[T].asRight
        }
      case '[User]            =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          new User(event.getOption(option).getAsUser).asInstanceOf[T].asRight
        }
      case '[Role]            =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          new Role(event.getOption(option).getAsRole).asInstanceOf[T].asRight
        }
      case '[Channel]         =>
        '{ (_: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          new Channel(event.getOption(option).getAsChannel.asGuildMessageChannel)
            .asInstanceOf[T]
            .asRight
        }

      case '[Option[?]] =>
        '{ (reader: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          println("option")
          Option(event.getOption(option))
            .flatTraverse(mapping => reader(mapping.getAsString).asInstanceOf[OptionResult[Option[T]]])
            .map(_.asInstanceOf[T])
        }
      case _            =>
        '{ (reader: OptionReader[T], event: SlashCommandInteractionEvent, option: String) =>
          println("ID")
          reader(event.getOption(option).getAsString)
        }
    }

  inline def getOption[T] = ${ fetchOption[T] }

  private def replyOptions[T: Type](using
    Quotes,
  ): Expr[(Show[T], CommandAutoCompleteInteractionEvent, List[T]) => IO[Unit]] =
    Type.of[T] match {
      case '[Int]    =>
        '{ (_: Show[T], event: CommandAutoCompleteInteractionEvent, options: List[T]) =>
          event.replyChoiceLongs(options.asInstanceOf[List[Int]].map(_.toLong)*).toIO.void
        }
      case '[Long]   =>
        '{ (_: Show[T], event: CommandAutoCompleteInteractionEvent, options: List[T]) =>
          event.replyChoiceLongs(options.asInstanceOf[List[Long]]*).toIO.void
        }
      case '[Double] =>
        '{ (_: Show[T], event: CommandAutoCompleteInteractionEvent, options: List[T]) =>
          event.replyChoiceDoubles(options.asInstanceOf[List[Double]]*).toIO.void
        }
      case _         => // String and others, using show
        '{ (show: Show[T], event: CommandAutoCompleteInteractionEvent, options: List[T]) =>
          given Show[T] = show
          event.replyChoiceStrings(options.map(_.show)*).toIO.void
        }
    }

  inline def replyChoices[T] = ${ replyOptions[T] }
