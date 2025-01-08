package cuteguard

import cuteguard.*
import cuteguard.Epoch.*
import cuteguard.RelativeTime.given
import cuteguard.model.Event
import cuteguard.model.discord.{DiscordID, User}

import cats.Show
import cats.effect.{FiberIO, IO, Ref}
import cats.instances.option.*
import cats.syntax.foldable.*
import cats.syntax.traverse.*

import scala.concurrent.duration.*

class EventEditor private (activeEditsRef: Ref[IO, Map[DiscordID, (FiberIO[Unit], Map[Int, Event])]]):
  def registerActiveEdit(user: User, edit: Map[Int, Event]): IO[Unit] =
    for
      remove      <- (IO.sleep(10.minutes) *> activeEditsRef.update(_ - user.discordID)).start
      activeEdits <- activeEditsRef.get
      _           <- activeEdits.get(user.discordID).traverse_(_(0).cancel)
      _           <- activeEditsRef.update(_ + (user.discordID -> (remove, edit)))
    yield ()

  def activeEdit(user: User): IO[Option[Map[Int, Event]]] =
    for
      remove      <- (IO.sleep(10.minutes) *> activeEditsRef.update(_ - user.discordID)).start
      activeEdits <- activeEditsRef.get
      edit        <- activeEdits.get(user.discordID).traverse { (oldRemove, edit) =>
                       for
                         _ <- oldRemove.cancel
                         _ <- activeEditsRef.update(_ + (user.discordID -> (remove, edit)))
                       yield edit
                     }
    yield edit

object EventEditor:
  def apply: IO[EventEditor] =
    Ref.of[IO, Map[DiscordID, (FiberIO[Unit], Map[Int, Event])]](Map.empty).map(new EventEditor(_))

  def formatEvents(eventMap: Map[Int, Event]): String =
    val header     = ("ID", "Amount", "Date", "Giver")
    val events     = header +:
      eventMap.toList.sortBy(_(0)).map { case (id, Event(_, _, issuer, _, amount, date)) =>
        val issuerText = issuer.fold("None")(_.fold("User left server")(_.nameInGuild))
        val dateText   = Show[RelativeTime].show(date.toEpoch.toRelativeTime)
        (id.toString, amount.toString, dateText, issuerText)
      }
    val maxLengths = events.foldLeft((0, 0, 0, 0)) {
      case ((idMax, amountMax, dateMax, giverMax), (id, amount, date, giver)) =>
        (
          math.max(idMax, id.length),
          math.max(amountMax, amount.length),
          math.max(dateMax, date.length),
          math.max(giverMax, giver.length),
        )
    }
    events.map { (id, amount, date, giver) =>
      List(
        id.padTo(maxLengths(0), ' '),
        amount.padTo(maxLengths(2), ' '),
        date.padTo(maxLengths(3), ' '),
        giver.padTo(maxLengths(1), ' '),
      ).mkString(" ")
    }.mkString("```", "\n", "```")
