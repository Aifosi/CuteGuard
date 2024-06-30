package cuteguard

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.derivation.default.derived
import pureconfig.module.catseffect.syntax.*

import scala.concurrent.duration.FiniteDuration

case class SubsmashConfiguration(
  minLength: Int,
  threshold: Float,
  activityReset: FiniteDuration,
) derives ConfigReader

case class CuteguardConfiguration(
  discord: DiscordConfiguration,
  subsmash: SubsmashConfiguration,
  cooldown: FiniteDuration,
) derives ConfigReader

object CuteguardConfiguration:
  def fromConfig(config: Config = ConfigFactory.load()): IO[CuteguardConfiguration] =
    ConfigSource.fromConfig(config).loadF()
