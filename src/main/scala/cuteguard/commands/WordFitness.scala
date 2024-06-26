package cuteguard.commands

import cuteguard.Fitness
import cuteguard.model.event.SlashCommandEvent

import cats.effect.IO
import org.typelevel.log4cats.Logger

case class WordFitness(fitness: Fitness) extends SlashCommand with Options:
  import fitness.*

  /** If set to false only admins can see it by default.
    */
  override val isUserCommand: Boolean = true
  override val fullCommand: String    = "fitness"

  override val options: List[PatternOption] = List(
    _.addOption[String]("text", "Text to calculate fitness of."),
  )

  override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    val text = event.getOption[String]("text")
    for
      (word, quadgramsWordFitness) <- text.sanitise.minWordFitness(0)
      _                            <-
        event.replyEphemeral(
          s"The word with the minimum fitness is `$word` with `${(quadgramsWordFitness * 100).round / 100d}`.",
        )
    yield true

  override val description: String = "Calculates the highest fitness of a word in the given text"
