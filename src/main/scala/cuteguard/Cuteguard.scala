package cuteguard

import cuteguard.Bot.Builder
import cuteguard.commands.*
import cuteguard.model.Discord

import cats.effect.{Deferred, IO, IOApp}
import org.typelevel.log4cats.Logger

//https://discord.com/api/oauth2/authorize?client_id=1207778654260822045&scope=bot+applications.commands&permissions=18432
//https://discord.com/oauth2/authorize?client_id=990221153203281950&scope=bot%20applications.commands&permissions=18432 - Test
object Cuteguard extends IOApp.Simple:

  private def commanderBuilder(
    config: CuteguardConfiguration,
  )(using discordLogger: DiscordLogger) = new Builder[Commander[DiscordLogger]]:
    override def apply(
      discord: Deferred[IO, Discord],
      grams: Grams,
    )(using Logger[IO]): Commander[DiscordLogger] =
      val commands: List[AnyCommand] = List(
        NotCute,
        // Subsmash,
        // LumiPats,
        Subsmash(grams, discord),
      )

      Commander(discordLogger, commands, discordLogger.complete(_, config))

  override def run: IO[Unit] =
    for
      config              <- CuteguardConfiguration.fromConfig()
      given DiscordLogger <- DiscordLogger.create
      _                   <- Bot.run(commanderBuilder(config))(using runtime)
    yield ()
