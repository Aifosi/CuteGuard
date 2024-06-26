package cuteguard.commands

import cuteguard.{Fitness, SubsmashConfiguration}
import cuteguard.model.Discord
import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.*
import cats.instances.option.*
import org.typelevel.log4cats.Logger

import java.time.Instant
import scala.util.matching.Regex

case class Subsmash(fitness: Fitness, discord: Deferred[IO, Discord], config: SubsmashConfiguration)
    extends TextCommand with NoLog:
  import fitness.*
  override def pattern: Regex = ".*".r

  private val lastActivationRef: Ref[IO, Option[(FiberIO[Unit], Instant)]] = Ref.unsafe(None)

  private val reset         = for
    _       <- IO.sleep(config.activityReset)
    discord <- discord.get
    _       <- discord.clearActivity.attempt
    _       <- lastActivationRef.set(None)
  yield ()
  private def resetActivity = for
    fiber <- reset.start
    _     <- lastActivationRef.set(Some(fiber, Instant.now))
  yield ()

  private def matches(messageText: String, memberNames: Set[String])(using Logger[IO]): IO[Boolean] =
    if messageText.length < config.minLength then return IO.pure(false)

    for
      (word, quadgramsWordFitness) <- memberNames
                                        .foldLeft(messageText)((filteredText, name) => filteredText.replace(name, ""))
                                        .replace("  ", " ")
                                        .minWordFitness(config.minLength)
      matches                      <-
        if quadgramsWordFitness > config.threshold then
          for
            _ <- Logger[IO].debug(s"Message Text: $messageText")
            _ <- Logger[IO].debug(s"word: $word, Fitness: $quadgramsWordFitness")
          yield true
        else IO.pure(false)
    yield matches

  private def activate(event: MessageEvent) = for
    discord <- discord.get
    _       <- discord.activity(s"Tormenting ${event.authorName}").attempt
    _       <- resetActivity
    embed    = Embed(
                 s"${event.authorName}, use your words cutie",
                 "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
                 "created by a sneaky totally not cute kitty",
               )
    _       <- event.reply(embed)
  yield ()

  private def run(event: MessageEvent): IO[Unit] = for
    lastActivation <- lastActivationRef.get
    _              <- lastActivation.fold(activate(event)) {
                        case (_, insant) if insant.plusSeconds(config.cooldown.toSeconds).isAfter(Instant.now) =>
                          IO.unit // cooldown
                        case (fiber, _)                                                                        =>
                          fiber.cancel *> activate(event)
                      }
  yield ()

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    for
      guild      <- event.guild
      members    <- guild.members.compile.toList
      memberNames = members.flatMap(_.guildName.sanitise.split(" ")).toSet
      matches    <- matches(event.content.sanitise, memberNames)
      _          <- IO.whenA(matches)(run(event))
    yield true

  override val description: String = "Responds when a user says they are not cute"
