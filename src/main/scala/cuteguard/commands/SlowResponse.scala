package cuteguard.commands

import cuteguard.model.discord.Message
import cuteguard.model.discord.event.{SlashAPI, SlashCommandEvent}

import cats.data.EitherT
import cats.effect.{IO, Ref}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.*

trait SlowResponse:
  this: SlashCommand =>
  val ephemeralResponses: Boolean
  def slowResponse(pattern: SlashPattern, event: SlashCommandEvent, slashAPI: Ref[IO, SlashAPI])(using
    Logger[IO],
  ): IO[Unit]

  final override def apply(pattern: SlashPattern, event: SlashCommandEvent)(using Logger[IO]): IO[Boolean] =
    def switchToHook(slashAPI: Ref[IO, SlashAPI], repliedRef: Ref[IO, Boolean]) =
      for
        _       <- IO.sleep(2.seconds)
        replied <- repliedRef.get
        _       <- IO.unlessA(replied) {
                     for
                       _ <- slashAPI.set(event.hook)
                       _ <- event.deferReply(ephemeralResponses)
                       _ <- repliedRef.set(true)
                     yield ()
                   }
      yield ()

    for
      slashAPI   <- Ref.of[IO, SlashAPI](event)
      repliedRef <- Ref.of[IO, Boolean](false)
      fiber      <- switchToHook(slashAPI, repliedRef).start
      _          <- slowResponse(pattern, event, slashAPI)
      _          <- repliedRef.set(true)
      _          <- fiber.cancel
    yield true

  protected def eitherTResponse(
    response: EitherT[IO, String, String],
    slashAPI: Ref[IO, SlashAPI],
  ): IO[Option[Message]] =
    for
      response     <- response.value
      slashAPI     <- slashAPI.get
      maybeMessage <-
        response.fold(
          error => slashAPI.replyEphemeral(error),
          response =>
            if ephemeralResponses then slashAPI.replyEphemeral(response) else slashAPI.reply(response).map(Some(_)),
        )
    yield maybeMessage
