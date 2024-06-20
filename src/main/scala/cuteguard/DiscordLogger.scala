package cuteguard

import cuteguard.model.{Channel, Discord, DiscordID, Message}
import cuteguard.syntax.io.*
import cuteguard.syntax.stream.*

import cats.data.OptionT
import cats.effect.{Deferred, IO, Ref}
import fs2.Stream
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*

open class DiscordLogger protected (
  notificationsRef: Ref[IO, Set[String]],
  logChannelDeferred: Deferred[IO, Option[Channel]],
):
  protected def log(channel: Deferred[IO, Option[Channel]], message: => String)(using
    Logger[IO],
  ): OptionT[IO, Message] =
    OptionT(channel.get).semiflatMap(_.sendMessage(message))

  protected def logStream(channel: Deferred[IO, Option[Channel]], message: => String)(using
    Logger[IO],
  ): Stream[IO, Unit] =
    Stream.eval(log(channel, message).value).as(())

  def logToChannel(message: => String)(using Logger[IO]): IO[Unit] = log(logChannelDeferred, message).value.void

  def logToChannelStream(message: => String)(using Logger[IO]): Stream[IO, Unit] =
    logStream(logChannelDeferred, message)

  def logWithoutSpam(message: => String)(using Logger[IO]): Stream[IO, Unit] =
    for
      notifications <- notificationsRef.get.streamed
      _             <- Stream.filter(!notifications.contains(message))
      _             <- notificationsRef.update(_ + message).streamed
      _             <- (IO.sleep(1.hour) *> notificationsRef.update(_ - message)).start.streamed
      n             <- logToChannelStream(message)
    yield n

  def complete(discord: Discord, config: CuteguardConfiguration)(using Logger[IO]): IO[Unit] =
    for
      logChannel <- DiscordLogger.getChannel(discord, "log", config.logChannelID)
      _          <- logChannelDeferred.complete(logChannel)
    yield ()

object DiscordLogger:
  def getChannel(discord: Discord, chanelName: String, id: Option[DiscordID])(using Logger[IO]): IO[Option[Channel]] =
    (for
      id      <- OptionT.fromOption(id).flatTapNone(Logger[IO].debug(s"$chanelName channel not configured."))
      channel <- discord
                   .channelByID(id)
                   .toOption
                   .flatTapNone(Logger[IO].debug(s"No channel with id $id found to assign for $chanelName channel."))
    yield channel).value

  def create: IO[DiscordLogger] =
    for
      notificationsRef   <- Ref.of[IO, Set[String]](Set.empty)
      logChannelDeferred <- Deferred[IO, Option[Channel]]
    yield new DiscordLogger(notificationsRef, logChannelDeferred)
