package cuteguard

import cuteguard.Fitness.sanitise

import cats.effect.{Deferred, IO}
import org.apache.commons.lang3.StringUtils.stripAccents

class Fitness(quadgrams: Deferred[IO, Map[String, Double]]):
  extension (string: String)
    private def fitness(gramLength: Int, gramScores: Map[String, Double]): Double = string match
      case string if string.isBlank || string.length < gramLength => 0d
      case string                                                 =>
        val grams             = string.sliding(gramLength).toList
        val longGramThreshold = 10
        val longGramPenalty   = Math.log(Math.max(string.length, longGramThreshold) - (longGramThreshold - Math.E))
        grams.foldLeft(0d)((acc, gram) => acc + gramScores(gram)) / grams.length * longGramPenalty

    private def maxWordFitness(minLength: Int, gram: Int, map: Map[String, Double]): (String, String, Double) =
      val minGramLength = Math.max(minLength, gram)
      string
        .split("( |\n)")
        .map { word =>
          val sanitised = word.sanitise
          val score     = if sanitised.length >= minGramLength then sanitised.fitness(gram, map) else 0d
          (word, sanitised, score)
        }
        .maxByOption(_(2))
        .getOrElse(("No Word", "", 0))

    def fitness: IO[Double] = quadgrams.get.map(string.fitness(4, _))

    def maxWordFitness(minLength: Int): IO[(String, String, Double)] =
      quadgrams.get.map(string.maxWordFitness(minLength, 4, _))

object Fitness:
  extension (string: String)
    def sanitise: String =
      stripAccents(string)                        // Remove diacritics
        .toLowerCase                              // to lowercase
        .replaceAll("https?://[^ ]+\\.[^ ]+", "") // remove links
        .replaceAll("<a?:\\w+:\\d+>", "")         // remove emoji
        .replaceAll("<sound:\\d+:\\d+>", "")      // remove sounds
        .replaceAll("`(:?``)?[^`]+`(:?``)?", "")  // remove code blocks
        .replaceAll("[^a-z \n]", "")              // Remove all symbols
        .replaceAll("(\\w)\\1{2,}", "$1")         // remove triples or longer
        .replaceAll("(\\w{2,})\\1+", "$1") // remove word repetitions
