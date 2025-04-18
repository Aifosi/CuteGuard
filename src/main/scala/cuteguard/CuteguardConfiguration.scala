package cuteguard

import cuteguard.model.discord.DiscordID

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.module.catseffect.syntax.*

import scala.concurrent.duration.FiniteDuration

case class SubsmashConfiguration(
  minLength: Int,
  threshold: Float,
  activityReset: FiniteDuration,
) derives ConfigReader

case class LinkConfiguration(
  subsmash: String,
  pleading: String,
  notCute: String,
) derives ConfigReader

case class CuteguardConfiguration(
  discord: DiscordConfiguration,
  postgres: PostgresConfiguration,
  subsmash: SubsmashConfiguration,
  links: LinkConfiguration,
  cooldown: FiniteDuration,
  guildID: DiscordID,
  counterChannelID: DiscordID,
) derives ConfigReader

object CuteguardConfiguration:
  def fromConfig(config: Config = ConfigFactory.load()): IO[CuteguardConfiguration] =
    ConfigSource.fromConfig(config).loadF()
