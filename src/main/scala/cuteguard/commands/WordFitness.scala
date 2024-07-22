package cuteguard.commands

import cuteguard.{Fitness, SubsmashConfiguration}
import cuteguard.model.discord.event.SlashCommandEvent
import cuteguard.syntax.eithert.*
import cuteguard.utils.toEitherT

import cats.data.EitherT
import cats.effect.IO
import org.typelevel.log4cats.Logger

case class WordFitness(fitness: Fitness, config: SubsmashConfiguration)
    extends SlashCommand with Options with ErrorMessages:

  import fitness.*

  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean = true
  override val fullCommand: String    = "fitness"

  override val options: List[PatternOption] = List(
    _.addOption[String]("text", "Text to calculate fitness of."),
  )

  override def run(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): EitherT[IO, String, Boolean] =
    for
      text            <- event.getOption[String]("text").toEitherT
      guild           <- EitherT.liftF(event.guild)
      members         <- EitherT.liftF(guild.members.compile.toList)
      memberNames      = members.flatMap(_.guildName.sanitise.split(" ")).toSet.filter(_.length >= 4)
      textAfterFilters = memberNames.foldLeft(text.sanitise)((filteredText, name) => filteredText.replace(name, ""))
      _               <- EitherT.leftWhen(textAfterFilters.isEmpty, "The text after filters are applied is empty.")
      _               <- EitherT.leftWhen(
                           textAfterFilters.length < 4,
                           s"The text after filters are applied is `$textAfterFilters`, it's too short to calculate " +
                             "it's fitness, a minimum length of 4 is needed.",
                         )

      (word, quadgramsWordFitness) <- EitherT.liftF(textAfterFilters.minWordFitness(0))

      fitness = (quadgramsWordFitness * 100).round / 100d
      isSmash = if quadgramsWordFitness > config.threshold then ", this is considered a subsmash" else ""

      _ <-
        EitherT.liftF(
          event.replyEphemeral(
            s"The word with the minimum fitness after filters are applied is `$word` with `$fitness`$isSmash.",
          ),
        )
    yield false

  override val description: String = "Calculates the highest fitness of a word in the given text"
