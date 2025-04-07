package cuteguard

import cuteguard.commands.*
import cuteguard.db.{DoobieLogHandler, Events, Preferences, Users}
import cuteguard.model.Action
import cuteguard.model.discord.{Channel, Discord}

import cats.data.EitherT
import cats.effect.*
import cats.effect.unsafe.IORuntime
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import fs2.io.file.{Files, Path}
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

//https://discord.com/api/oauth2/authorize?client_id=1207778654260822045&scope=bot+applications
// .commands&permissions=18432
//https://discord.com/oauth2/authorize?client_id=990221153203281950&scope=bot%20applications
// .commands&permissions=18432 - Test
object Cuteguard extends IOApp.Simple:
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
    // This makes it so if a quadgram is not found it's considered very unlikely in english
    .map(_.toMap.withDefaultValue(10d))
    .flatMap(gramsDeferred.complete)
    .flatMap(_ => Logger[IO].info("Loaded Quadgrams"))
    .start
    .void

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

  private def addCommands(
    subsmashConfiguration: SubsmashConfiguration,
    links: LinkConfiguration,
    discord: Deferred[IO, Discord],
    quadgrams: Deferred[IO, Map[String, Double]],
    cooldown: Cooldown,
    events: Events,
    counterChannel: IO[Channel],
    eventEditor: EventEditor,
    preferences: Preferences,
  )(using Logger[IO]): Commander =
    val fitness                    = Fitness(quadgrams)
    val commands: List[AnyCommand] = List(
      NotCute(cooldown, preferences, links.notCute),
      Pleading(cooldown, preferences, links.pleading),
      Subsmash(cooldown, preferences, fitness, discord, subsmashConfiguration, links.subsmash),
      CheckSubsmash(fitness, subsmashConfiguration),
      ActionCommand(events, counterChannel, Action.Edge),
      ActionCommand(events, counterChannel, Action.Ruin),
      ActionCommand(events, counterChannel, Action.Orgasm),
      ActionCommand(events, counterChannel, Action.WetDream),
      ActionCommand(events, counterChannel, Action.Spank),
      Total(events),
      Highscore(events),
      Last(events),
      EventList(events, eventEditor),
      EventDelete(events, eventEditor),
      EventEdit(events, eventEditor),
      OptCommand(preferences, true),
      OptCommand(preferences, false),
    )

    Commander(commands)

  override def run: IO[Unit] =
    for
      given Logger[IO] <- Slf4jLogger.create[IO]
      config           <- CuteguardConfiguration.fromConfig()
      _                <- runMigrations(config.postgres)

      discordDeferred <- Deferred[IO, Discord]
      gramsDeferred   <- Deferred[IO, Map[String, Double]]

      given LogHandler[IO] <- DoobieLogHandler.create
      given Transactor[IO]  = config.postgres.transactor
      given DiscordLogger  <- DiscordLogger(discordDeferred, config.discord)

      guild          = EitherT.liftF(discordDeferred.get).flatMap(_.guildByID(config.guildID))
      counterChannel = EitherT.liftF(discordDeferred.get).flatMap(_.channelByID(config.counterChannelID)).value.rethrow

      users       = Users(guild)
      events      = Events(users)
      preferences = Preferences(users)

      cooldown       <- Cooldown(config.cooldown, events)
      eventEditor    <- EventEditor.apply
      commander       =
        addCommands(
          config.subsmash,
          config.links,
          discordDeferred,
          gramsDeferred,
          cooldown,
          events,
          counterChannel,
          eventEditor,
          preferences,
        )
      given IORuntime = runtime
      messageListener = new MessageListener(commander)
      _              <- loadQuadgrams(gramsDeferred)
      releaseDiscord <- acquireDiscordClient(discordDeferred, config.discord.token, messageListener)
      _              <- commander.registerSlashCommands(discordDeferred).start
      _              <- IO.never
      _              <- releaseDiscord
    yield ()
