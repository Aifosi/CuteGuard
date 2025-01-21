package cuteguard

import cats.effect.{Deferred, IO}

class Fitness(quadgrams: Deferred[IO, Map[String, Double]]):
  extension (string: String)
    private def fitness(gramLength: Int, gramScores: Map[String, Double]): Double = string match
      case string if string.isBlank || string.length < gramLength => 0d
      case string                                                 =>
        val grams = string.sliding(gramLength).toList
        grams.foldLeft(0d)((acc, gram) => acc + gramScores(gram)) / (string.length - grams.length)

    private def maxWordFitness(minLength: Int, gram: Int, map: Map[String, Double]): (String, Double) =
      val minGramLength = Math.max(minLength, gram)
      string
        .split("( |\n)")
        .collect {
          case word if word.length >= minGramLength =>
            word -> word.fitness(gram, map)
        }
        .maxByOption(_(1))
        .getOrElse("No Word" -> 0)

    def fitness: IO[Double] = quadgrams.get.map(string.fitness(4, _))

    def maxWordFitness(minLength: Int): IO[(String, Double)] = quadgrams.get.map(string.maxWordFitness(minLength, 4, _))
