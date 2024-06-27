package cuteguard

import cats.effect.{Deferred, IO}
import org.apache.commons.lang3.StringUtils.stripAccents

class Fitness(quadgrams: Deferred[IO, Map[String, Double]]):
  extension (string: String)
    private def fitness(gram: Int, map: Map[String, Double]): Double                                  =
      if string.isBlank then 0d
      else string.sliding(gram).foldLeft(0d)((acc, trigram) => acc + map(trigram)) / string.length
    private def minWordFitness(minLength: Int, gram: Int, map: Map[String, Double]): (String, Double) =
      val minGramLength = Math.min(minLength, gram)
      string
        .split("( |\n)")
        .collect {
          case word if word.length >= minGramLength =>
            word -> word.fitness(gram, map)
        }
        .maxByOption(_(1))
        .getOrElse("No Word" -> 0)

    def fitness: IO[Double] = quadgrams.get.map(fitness(4, _))

    def minWordFitness(minLength: Int): IO[(String, Double)] = quadgrams.get.map(minWordFitness(minLength, 4, _))

    def sanitise: String =
      stripAccents(string)                          // Remove diacritics
        .toLowerCase                                // to lowercase
        .replaceAll("https?://[^ ]+\\.[^ ]+", "")   // remove links
        .replaceAll("<a?:\\w+:\\d+>", "")           // remove emoji and links
        .replaceAll("`(:?``)?[^`]+`(:?``)?", "")    // remove code blocks
        .replaceAll("(\\w+)[^\\w ](\\w+)", "$1 $2") // remove word alternations
        .replaceAll("(\\w)\\1+", "$1")              // remove single character repetitions
        .replaceAll("(\\w\\w)\\1+", "$1")           // remove doube character repetitions
        .replaceAll("[^a-z \n]", "") // Remove all symbols
