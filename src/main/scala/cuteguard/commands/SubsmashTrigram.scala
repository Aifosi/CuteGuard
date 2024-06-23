package cuteguard.commands

import cuteguard.Grams
import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

//TODO do not retrigger too fast for the same user, maybe some 5 sec cooldown?
case class SubsmashTrigram(grams: Grams) extends TextCommand with NoLog:
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
        .replaceAll("https?://[^ ]+", "")          // remove links
        .replaceAll("<.+?>", "")                   // remove emoji and links
        .replaceAll(":.+?:", "")                   // remove more emoji
        .replaceAll("`.+?`(:?``)?", "")            // remove code blocks
        .replaceAll("(\\w+)[^a-z](\\w+)", "$1 $2") // remove word alternations
        .replaceAll("[^a-z \n]", "")

    val minLength = 6
    if filteredText.length < minLength then false
    else
      val (word, quadgramsWordFitness) = filteredText.minWordFitness(minLength, 4, grams.quadgrams)
      if quadgramsWordFitness > 3.5 then
        println(s"filteredText: $filteredText")
        println(s"word: $word")
        println(s"quadgramsWordFitness: $quadgramsWordFitness")
        true
      else false
      /*val trigramWordFitness = filteredText.minWordFitness(minLength, 3, grams.trigrams)
      val trigramFitness = filteredText.replaceAll(" ", "").fitness(3, grams.trigrams)
      val quadgramsWordFitness = filteredText.minWordFitness(minLength, 4, grams.quadgrams)
      val quadgramsFitness = filteredText.replaceAll(" ", "").fitness(4, grams.quadgrams)
      println(s"trigramWordFitness: $trigramWordFitness")
      println(s"trigramFitness: $trigramFitness")
      println(s"quadgramsWordFitness: $quadgramsWordFitness")
      println(s"quadgramsFitness: $quadgramsFitness")*/

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] = {
    val embed = Embed(
      s"${event.authorName}, use your words cutie",
      "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
      "created by a sneaky totally not cute kitty",
    )
    event.reply(embed).as(true)
  }

  override val description: String = "Responds when a user says they are not cute"
