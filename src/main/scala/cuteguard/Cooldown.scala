package cuteguard

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.{DiscordID, User}

import cats.data.OptionT
import cats.effect.{IO, Ref}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

class Cooldown(cooldownsRef: Ref[IO, Map[DiscordID, Instant]], cooldown: FiniteDuration, events: Events):
  def addEventAndCheckReady(user: User, action: Action, giver: OptionT[IO, User] = OptionT.none): IO[Boolean] =
    for
      cooldowns <- cooldownsRef.get
      giver     <- giver.value
      isReady    = !cooldowns.get(user.discordID).fold(false)(_.plusSeconds(cooldown.toSeconds).isAfter(Instant.now))
      addEvent   = events.add(user, giver, action, 1, None, label = Some(s"cooldown for ${action.show}"))
      _         <- addEvent *> IO.whenA(isReady)(cooldownsRef.update(_ + (user.discordID -> Instant.now)))
    yield isReady

object Cooldown:
  def apply(cooldown: FiniteDuration, events: Events): IO[Cooldown] =
    Ref.of[IO, Map[DiscordID, Instant]](Map.empty).map(new Cooldown(_, cooldown, events))
