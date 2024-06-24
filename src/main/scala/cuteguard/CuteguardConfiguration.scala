package cuteguard

import cuteguard.model.DiscordID

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.derived
import pureconfig.module.catseffect.syntax.*

import scala.concurrent.duration.FiniteDuration

case class SubsmashConfiguration(
  minLength: Int,
  threshold: Float,
  cooldown: FiniteDuration,
  activityReset: FiniteDuration,
) derives ConfigReader

case class CuteguardConfiguration(
  logChannelID: Option[DiscordID],
  subsmash: SubsmashConfiguration,
) derives ConfigReader

object CuteguardConfiguration:

  given ConfigReader[(Int, DiscordID)]                                              = ConfigReader.derived[(Int, DiscordID)]
  def fromConfig(config: Config = ConfigFactory.load()): IO[CuteguardConfiguration] =
    ConfigSource.fromConfig(config).at("cuteguard").loadF()
