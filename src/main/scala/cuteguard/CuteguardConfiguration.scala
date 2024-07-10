package cuteguard

import cuteguard.model.discord.DiscordID

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
  postgres: PostgresConfiguration,
  subsmash: SubsmashConfiguration,
  cooldown: FiniteDuration,
  guildID: DiscordID,
  counterChannelID: DiscordID,
) derives ConfigReader

object CuteguardConfiguration:
  def fromConfig(config: Config = ConfigFactory.load()): IO[CuteguardConfiguration] =
    ConfigSource.fromConfig(config).loadF()
