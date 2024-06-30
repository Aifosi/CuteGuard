package cuteguard

import cuteguard.model.DiscordID
import cuteguard.model.User

import cats.effect.{IO, Ref}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

class Cooldown(cooldownsRef: Ref[IO, Map[DiscordID, Instant]], cooldown: FiniteDuration):
  def interact(user: User)(interaction: IO[Unit]): IO[Boolean] =
    for
      cooldowns   <- cooldownsRef.get
      isOnCooldown = cooldowns.get(user.discordID).fold(false)(_.plusSeconds(cooldown.toSeconds).isAfter(Instant.now))
      _           <- IO.unlessA(isOnCooldown)(cooldownsRef.update(_ + (user.discordID -> Instant.now)) *> interaction)
    yield isOnCooldown

object Cooldown:
  def apply(cooldown: FiniteDuration): IO[Cooldown] =
    Ref.of[IO, Map[DiscordID, Instant]](Map.empty).map(new Cooldown(_, cooldown))
