package cuteguard

import cuteguard.Bot.Builder
import cuteguard.commands.{AnyCommand, AnyMessage}
import cuteguard.db.CuteGuardUserRepository
import cuteguard.model.Discord

import cats.effect.{Deferred, IO, IOApp}
import doobie.{LogHandler, Transactor}
import org.typelevel.log4cats.Logger

//https://discord.com/api/oauth2/authorize?client_id=1207778654260822045&scope=applications.commands&permissions=268438528
object Cuteguard extends IOApp.Simple:

  private def commanderBuilder(
    config: CuteguardConfiguration,
  )(using discordLogger: DiscordLogger) = new Builder[Commander[DiscordLogger]]:
    override def apply(
      discord: Deferred[IO, Discord],
      cuteGuardUserRepository: CuteGuardUserRepository,
    )(using Transactor[IO], LogHandler, Logger[IO]): Commander[DiscordLogger] =
      val points = Points(discord, cuteGuardUserRepository, config)

      val commands: List[AnyCommand] = List(
        AnyMessage(points, config.pointsPerMessage),
      )

      Commander(discordLogger, commands, discordLogger.complete(_, config))

  override def run: IO[Unit] =
    for
      config              <- CuteguardConfiguration.fromConfig()
      given DiscordLogger <- DiscordLogger.create
      _                   <- Bot.run(commanderBuilder(config))(using runtime)
    yield ()
