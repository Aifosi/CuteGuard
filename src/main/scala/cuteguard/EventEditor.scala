package cuteguard

import cuteguard.commands.ActionCommand
import cuteguard.model.Event
import cuteguard.model.discord.{DiscordID, User}

import cats.effect.{FiberIO, IO, Ref}
import cats.instances.option.*
import cats.syntax.foldable.*
import cats.syntax.traverse.*

import java.time.{LocalDate, ZoneOffset}
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
    val header     = ("ID", "Giver", "Amount", "Date")
    val events     = header +:
      eventMap.toList.map { case (id, Event(_, _, issuer, _, amount, date)) =>
        val issuerText = issuer.fold("User left server")(_.fold("None")(_.nameInGuild))
        val dateText   = LocalDate.ofInstant(date, ZoneOffset.UTC).format(ActionCommand.dateTimeFormatter)
        (id.toString, issuerText, amount.toString, dateText)
      }
    val maxLengths = events.foldLeft((0, 0, 0, 0)) {
      case ((idMax, giverMax, amountMax, dateMax), (id, giver, amount, date)) =>
        (
          math.max(idMax, id.length),
          math.max(giverMax, giver.length),
          math.max(amountMax, amount.length),
          math.max(dateMax, date.length),
        )
    }
    events.map { (id, giver, amount, date) =>
      List(
        id.padTo(maxLengths(0), ' '),
        giver.padTo(maxLengths(1), ' '),
        amount.padTo(maxLengths(2), ' '),
        date.padTo(maxLengths(3), ' '),
      ).mkString(" ")
    }.mkString("```", "\n", "```")
