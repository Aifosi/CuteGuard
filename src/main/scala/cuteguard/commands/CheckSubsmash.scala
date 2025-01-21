package cuteguard.commands

import cuteguard.{Fitness, SubsmashConfiguration}
import cuteguard.model.discord.event.SlashCommandEvent
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import org.typelevel.log4cats.Logger

case class CheckSubsmash(fitness: Fitness, config: SubsmashConfiguration)
    extends SlashCommand with Options with ErrorMessages:

  import fitness.*

  override val fullCommand: String = "check subsmash"

  override val options: List[PatternOption] = List(
    _.addOption[String]("text", "Text to check for a subsmash."),
  )

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      text <- event.getOption[String]("text").toEitherT

      (word, fitnessScore) <- EitherT(
                                Subsmash
                                  .best(fitness, config)(text, event.guild)
                                  .map(_.toRight("The text after filters are applied is empty.")),
                              )

      fitness = (fitnessScore * 100).round / 100d

      _ <-
        EitherT.liftF(
          event.replyEphemeral(
            s"The word with the highest score is `$word` with `$fitness` a points, `${config.threshold}` is the " +
              "minimum score needed for text to be considered a subsmash.",
          ),
        )
    yield false

  override val description: String =
    "Checks text for subsmashes and tells you the score of the word with the highest score."
