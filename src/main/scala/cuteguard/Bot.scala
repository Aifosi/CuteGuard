package cuteguard

import cuteguard.model.Discord

import cats.effect.*
import cats.effect.unsafe.IORuntime
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Bot:
  private def acquireDiscordClient(
    token: String,
    messageListener: MessageListener,
  )(using Logger[IO]): Resource[IO, Discord] =
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
      .evalTap(_ => Logger[IO].info("Loaded JDA"))

  private def loadConfigs: IO[DiscordConfiguration] =
    for discordConfig <- DiscordConfiguration.fromConfig()
    yield discordConfig

  abstract class Builder[A]:
    def apply(
      discord: Deferred[IO, Discord],
      grams: Grams,
    )(using Logger[IO]): A

  def run[Log <: DiscordLogger](
    commanderBuilder: Builder[Commander[Log]],
  )(using IORuntime): IO[Unit] =
    for
      given Logger[IO] <- Slf4jLogger.create[IO]
      discordConfig    <- loadConfigs

      discordDeferred <- Deferred[IO, Discord]

      grams <- Grams.apply

      commander = commanderBuilder(discordDeferred, grams)

      given Log = commander.logger

      commanderWithDefaults = commander.withDefaults

      messageListener            = new MessageListener(commanderWithDefaults)
      (discord, releaseDiscord) <- acquireDiscordClient(discordConfig.token, messageListener).allocated
      _                         <- discordDeferred.complete(discord)
      _                         <- commander.onDiscordAcquired(discord)
      _                         <- commanderWithDefaults.registerSlashCommands(discord).start
      _                         <- IO.never
      _                         <- releaseDiscord
    yield ()
