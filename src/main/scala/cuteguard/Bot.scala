package cuteguard

import cuteguard.model.Discord

import cats.effect.*
import cats.effect.unsafe.IORuntime
import fs2.io.file.{Files, Path}
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration

object Bot:
  private def acquireDiscordClient(
    discordDeferred: Deferred[IO, Discord],
    token: String,
    messageListener: MessageListener,
  )(using Logger[IO]): IO[IO[Unit]] =
    val acquire = IO {
      JDABuilder
        .createDefault(token)
        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
        .addEventListeners(messageListener)
        .build()
        .awaitReady()
    }

    Resource
      .make(acquire)(jda => IO(jda.shutdown()))
      .map(new Discord(_))
      .evalMap(discordDeferred.complete)
      .evalTap(_ => Logger[IO].info("Loaded JDA"))
      .allocated
      .map(_(1))

  private def loadQuadgrams(gramsDeferred: Deferred[IO, Map[String, Double]])(using Logger[IO]): IO[Unit] = Files[IO]
    .readUtf8Lines(Path("quadgrams.csv"))
    .map { string =>
      val split = string.split(",")
      (split.head.toLowerCase, split(1).toDouble)
    }
    .compile
    .toList
    .map(_.toMap.withDefaultValue(0d))
    .flatMap(gramsDeferred.complete)
    .flatMap(_ => Logger[IO].info("Loaded Quadgrams"))
    .start
    .void

  abstract class Builder[A]:
    def apply(
      discord: Deferred[IO, Discord],
      quadgrams: Deferred[IO, Map[String, Double]],
      cooldown: Cooldown,
    )(using Logger[IO]): A

  def run[Log <: DiscordLogger](
    discordConfig: DiscordConfiguration,
    discordDeferred: Deferred[IO, Discord],
    cooldown: FiniteDuration,
    commanderBuilder: Builder[Commander[Log]],
  )(using IORuntime, Logger[IO]): IO[Unit] =
    for
      gramsDeferred <- Deferred[IO, Map[String, Double]]

      cooldown <- Cooldown(cooldown)

      commander = commanderBuilder(discordDeferred, gramsDeferred, cooldown)

      given Log = commander.logger

      commanderWithDefaults = commander.withDefaults

      messageListener = new MessageListener(commanderWithDefaults)
      _              <- loadQuadgrams(gramsDeferred)
      releaseDiscord <- acquireDiscordClient(discordDeferred, discordConfig.token, messageListener)
      _              <- commanderWithDefaults.registerSlashCommands(discordDeferred).start
      _              <- IO.never
      _              <- releaseDiscord
    yield ()
