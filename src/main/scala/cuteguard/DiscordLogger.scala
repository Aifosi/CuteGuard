package cuteguard

import cuteguard.model.{Channel, Discord, DiscordID}
import cuteguard.syntax.io.*
import cuteguard.syntax.stream.*

import cats.data.OptionT
import cats.effect.{Deferred, IO, Ref}
import fs2.Stream
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*

open class DiscordLogger protected (
  notificationsRef: Ref[IO, Set[String]],
  getLogChannel: () => OptionT[IO, Channel],
):
  protected def logStream(channel: Channel, message: => String): Stream[IO, Unit] =
    Stream.eval(channel.sendMessage(message)).as(())

  def logToChannel(message: => String): IO[Unit] =
    getLogChannel().semiflatMap(_.sendMessage(message)).value.void

  def logToChannelStream(message: => String): Stream[IO, Unit] =
    Stream.evals(getLogChannel().value).flatMap(logStream(_, message))

  def logWithoutSpam(message: => String): Stream[IO, Unit] =
    for
      notifications <- notificationsRef.get.streamed
      _             <- Stream.filter(!notifications.contains(message))
      _             <- notificationsRef.update(_ + message).streamed
      _             <- (IO.sleep(1.hour) *> notificationsRef.update(_ - message)).start.streamed
      _             <- logToChannelStream(message)
    yield ()

object DiscordLogger:
  def getChannel(discord: Discord, chanelName: String, id: Option[DiscordID])(using Logger[IO]): OptionT[IO, Channel] =
    for
      id      <- OptionT.fromOption(id).flatTapNone(Logger[IO].debug(s"$chanelName channel not configured."))
      channel <- discord
                   .channelByID(id)
                   .toOption
                   .flatTapNone(Logger[IO].debug(s"No channel with id $id found to assign for $chanelName channel."))
    yield channel

  def getLogChannel(
    discordDeferred: Deferred[IO, Discord],
    config: DiscordConfiguration,
  )(using Logger[IO]): OptionT[IO, Channel] = for
    discord <- OptionT.liftF(discordDeferred.get)
    channel <- getChannel(discord, "log", config.logChannelID)
    _       <- OptionT.liftF(Logger[IO].info(s"Log channel is ${channel.name}"))
  yield channel

  def apply(discordDeferred: Deferred[IO, Discord], config: DiscordConfiguration)(using Logger[IO]): IO[DiscordLogger] =
    for notificationsRef <- Ref.of[IO, Set[String]](Set.empty)
    yield new DiscordLogger(notificationsRef, () => getLogChannel(discordDeferred, config))
