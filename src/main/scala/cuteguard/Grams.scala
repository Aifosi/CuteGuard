package cuteguard

import cats.effect.IO
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.Logger

case class Grams(trigrams: Map[String, Double], quadgrams: Map[String, Double])

object Grams:
  def read(path: String): IO[Map[String, Double]] = Files[IO]
    .readUtf8Lines(Path(path))
    .map { string =>
      val split = string.split(",")
      (split.head.toLowerCase, split(1).toDouble)
    }
    .compile
    .toList
    .map(_.toMap.withDefaultValue(0d))
  def apply(using Logger[IO]): IO[Grams]          = for
    _         <- Logger[IO].info("Parsing grams")
    trigrams  <- read("trigrams.csv")
    quadgrams <- read("quadgrams.csv")
    _         <- Logger[IO].info("Parsing complete")
  yield Grams(trigrams, quadgrams)
