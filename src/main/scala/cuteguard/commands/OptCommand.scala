package cuteguard.commands

import cuteguard.*
import cuteguard.commands.AutoCompletable.*
import cuteguard.db.Preferences
import cuteguard.model.{Action, CanOpt}
import cuteguard.model.discord.event.{AutoCompleteEvent, SlashCommandEvent}
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.typelevel.log4cats.Logger

case class OptCommand(preferences: Preferences, out: Boolean)
    extends SlashCommand with Options with ErrorMessages with AutoComplete[Action & CanOpt]:
  override val description: String =
    "Opts of tracking and receiving replies for chat actions."

  override val fullCommand: String          = s"opt ${if out then "out" else "in"}"
  override val options: List[PatternOption] = List(
    _.addOption[Action & CanOpt](
      "action",
      s"What do you want to opt ${if out then "out of" else "in to"}?",
      autoComplete = true,
    ),
  )

  override val autoCompleteOptions: Map[String, AutoCompleteEvent => IO[List[Action & CanOpt]]] = Map(
    "action" -> CanOpt.values.toList,
  ).fromSimplePure

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      action       <- event.getOption[Action & CanOpt]("action").toEitherT
      preference   <- EitherT.liftF(preferences.find(event.author).value)
      label         = Some(if out then "opt out" else "opt in")
      addPreference = preferences.add(
                        event.author,
                        pleadingOptOut = action == Action.Pleading && out,
                        subsmashOptOut = action == Action.Subsmash && out,
                        notCuteOptOut = action == Action.NotCute && out,
                        label,
                      )
      _            <- EitherT.liftF {
                        preference.fold(addPreference) { preference =>
                          preferences.update(
                            preference.id,
                            pleadingOptOut = Option.when(action == Action.Pleading)(out),
                            subsmashOptOut = Option.when(action == Action.Subsmash)(out),
                            notCuteOptOut = Option.when(action == Action.NotCute)(out),
                            label,
                          )
                        }
                      }
      message       = if out then s"Cuteguard will no longer track or reply to your ${action.plural}!"
                      else s"Cuteguard will now once again track and reply to your ${action.plural}!"
      _            <- EitherT.liftF(event.replyEphemeral(message))
    yield true
