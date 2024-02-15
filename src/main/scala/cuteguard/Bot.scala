package cuteguard

import cuteguard.db.{CuteGuardUserRepository, DoobieLogHandler}
import cuteguard.model.Discord

import cats.effect.*
import cats.effect.unsafe.IORuntime
import doobie.{LogHandler, Transactor}
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.flywaydb.core.Flyway
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

  private def runMigrations(postgresConfig: PostgresConfiguration)(using Logger[IO]): IO[Unit] =
    for
      flyway     <- IO {
                      Flyway.configure
                        .dataSource(postgresConfig.url, postgresConfig.user, postgresConfig.password)
                        .validateMigrationNaming(true)
                        .baselineOnMigrate(true)
                        .load
                    }
      migrations <- IO(flyway.migrate())
      _          <- Logger[IO].debug(s"Ran ${migrations.migrationsExecuted} migrations.")
    yield ()

  private def loadConfigs: IO[(DiscordConfiguration, PostgresConfiguration)] =
    for
      discordConfig <- DiscordConfiguration.fromConfig()
      postgres      <- PostgresConfiguration.fromConfig()
    yield (discordConfig, postgres)

  abstract class Builder[A]:
    def apply(
      discord: Deferred[IO, Discord],
      cuteGuardUserRepository: CuteGuardUserRepository,
    )(using Transactor[IO], LogHandler, Logger[IO]): A

  def run[Log <: DiscordLogger](
    commanderBuilder: Builder[Commander[Log]],
  )(using IORuntime): IO[Unit] =
    for
      given Logger[IO]                <- Slf4jLogger.create[IO]
      (discordConfig, postgresConfig) <- loadConfigs

      _ <- runMigrations(postgresConfig)

      given Transactor[IO] = postgresConfig.transactor
      given LogHandler    <- DoobieLogHandler.default
      discordDeferred     <- Deferred[IO, Discord]

      cuteGuardUserRepository = new CuteGuardUserRepository(discordDeferred)

      commander = commanderBuilder(discordDeferred, cuteGuardUserRepository)

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
