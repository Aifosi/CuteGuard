package cuteguard

import cuteguard.db.Events
import cuteguard.model.Action
import cuteguard.model.discord.{DiscordID, User}

import cats.effect.{IO, Ref}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

class Cooldown(cooldownsRef: Ref[IO, Map[DiscordID, Instant]], cooldown: FiniteDuration, events: Events):
  def addEventAndCheckReady(user: User, action: Action): IO[Boolean] =
    for
      cooldowns <- cooldownsRef.get
      isReady    = !cooldowns.get(user.discordID).fold(false)(_.plusSeconds(cooldown.toSeconds).isAfter(Instant.now))
      addEvent   = events.add(user, None, action, 1, None, label = Some(s"cooldown for ${action.show}"))
      _         <- addEvent *> IO.whenA(isReady)(cooldownsRef.update(_ + (user.discordID -> Instant.now)))
    yield isReady

  def interact(user: User)(action: Action, interaction: IO[Unit]): IO[Boolean] =
    addEventAndCheckReady(user, action).flatTap(IO.whenA(_)(interaction))

object Cooldown:
  def apply(cooldown: FiniteDuration, events: Events): IO[Cooldown] =
    Ref.of[IO, Map[DiscordID, Instant]](Map.empty).map(new Cooldown(_, cooldown, events))
