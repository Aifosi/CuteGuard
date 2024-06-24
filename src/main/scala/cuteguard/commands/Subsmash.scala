package cuteguard.commands

import cuteguard.Grams
import cuteguard.SubsmashConfiguration
import cuteguard.model.Discord
import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.*
import cats.instances.option.*
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import java.time.Instant
import scala.util.matching.Regex

case class Subsmash(grams: Grams, discord: Deferred[IO, Discord], config: SubsmashConfiguration)
    extends TextCommand with NoLog:
  private val lastActivationRef: Ref[IO, Option[(FiberIO[Unit], Instant)]] = Ref.unsafe(None)

  private val reset         = for
    _       <- IO.sleep(config.activityReset)
    discord <- discord.get
    _       <- discord.clearActivity.attempt
    _       <- lastActivationRef.set(None)
  yield ()
  private def resetActivity = for
    fiber <- reset.start
    _     <- lastActivationRef.set(Some(fiber, Instant.now))
  yield ()

  override def pattern: Regex = ".*".r

  extension (string: String)
    def fitness(gram: Int, map: Map[String, Double]): Double                                  =
      if string.isBlank then 0d
      else string.sliding(gram).foldLeft(0d)((acc, trigram) => acc + map(trigram)) / string.length
    def minWordFitness(minLength: Int, gram: Int, map: Map[String, Double]): (String, Double) =
      string
        .split("( |\n)")
        .collect {
          case word if word.length >= minLength =>
            word -> word.fitness(gram, map)
        }
        .maxByOption(_(1))
        .getOrElse("No Word" -> 0)

  override def matches(event: MessageEvent): Boolean =
    val filteredText =
      stripAccents(event.content)                  // Remove diacritics
        .toLowerCase                               // to lowercase
        .replaceAll("https?://[^ ]+\\.[^ ]+", "")  // remove links
        .replaceAll("<.+?>", "")                   // remove emoji and links
        .replaceAll(":.+?:", "")                   // remove more emoji
        .replaceAll("`.+?`(:?``)?", "")            // remove code blocks
        .replaceAll("(\\w+)[^a-z](\\w+)", "$1 $2") // remove word alternations
        .replaceAll("(\\w)\\1+", "$1")             // remove single character repetitions
        .replaceAll("(\\w\\w)\\1+", "$1")          // remove doube character repetitions
        .replaceAll("[^a-z \n]", "") // Remove all symbols

    if filteredText.length < config.minLength then false
    else
      val (word, quadgramsWordFitness) = filteredText.minWordFitness(config.minLength, 4, grams.quadgrams)
      if quadgramsWordFitness > 3.5 then
        println(s"filteredText: $filteredText")
        println(s"word: $word")
        println(s"quadgramsWordFitness: $quadgramsWordFitness")
        true
      else false

  private def activate(event: MessageEvent) = for
    discord <- discord.get
    _       <- discord.activity(s"Tormenting ${event.authorName}").attempt
    _       <- resetActivity
    embed    = Embed(
                 s"${event.authorName}, use your words cutie",
                 "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
                 "created by a sneaky totally not cute kitty",
               )
    _       <- event.reply(embed)
  yield ()

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    for
      lastActivation <- lastActivationRef.get
      _              <- lastActivation.fold(activate(event)) {
                          case (_, insant) if insant.plusSeconds(config.cooldown.toSeconds).isAfter(Instant.now) =>
                            IO.unit // cooldown
                          case (fiber, _)                                                                        =>
                            fiber.cancel *> activate(event)
                        }
    yield true

  override val description: String = "Responds when a user says they are not cute"
