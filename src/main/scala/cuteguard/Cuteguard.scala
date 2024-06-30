package cuteguard

import cuteguard.Bot.Builder
import cuteguard.commands.*
import cuteguard.model.Discord

import cats.effect.{Deferred, IO, IOApp}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

//https://discord.com/api/oauth2/authorize?client_id=1207778654260822045&scope=bot+applications.commands&permissions=18432
//https://discord.com/oauth2/authorize?client_id=990221153203281950&scope=bot%20applications.commands&permissions=18432 - Test
object Cuteguard extends IOApp.Simple:

  private def commanderBuilder(
    config: CuteguardConfiguration,
  )(using discordLogger: DiscordLogger) = new Builder[Commander[DiscordLogger]]:
    override def apply(
      discord: Deferred[IO, Discord],
      quadgrams: Deferred[IO, Map[String, Double]],
    )(using Logger[IO]): Commander[DiscordLogger] =
      val fitness                    = Fitness(quadgrams)
      val commands: List[AnyCommand] = List(
        NotCute,
        // Subsmash,
        // LumiPats,
        Subsmash(fitness, discord, config.subsmash),
        Pleading,
      )

      Commander(discordLogger, commands)

  override def run: IO[Unit] =
    for
      given Logger[IO]    <- Slf4jLogger.create[IO]
      config              <- CuteguardConfiguration.fromConfig()
      discordDeferred     <- Deferred[IO, Discord]
      given DiscordLogger <- DiscordLogger(discordDeferred, config.discord)
      _                   <- Bot.run(config.discord, discordDeferred, commanderBuilder(config))(using runtime)
    yield ()
