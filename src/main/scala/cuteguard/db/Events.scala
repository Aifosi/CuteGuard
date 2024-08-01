package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.{Action, Event as CuteguardEvent}
import cuteguard.model.discord.User as DiscordUser
import cuteguard.syntax.localdate.*
import cuteguard.utils.Maybe

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.instances.option.*
import cats.syntax.traverse.*
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.string.*

import java.time.{Instant, LocalDate}
import java.util.UUID
case class Event(
  id: UUID,
  receiverUserId: UUID,
  issuerUserId: Option[UUID],
  action: Action,
  amount: Int,
  date: Instant,
)

class Events(users: Users)(using Transactor[IO]) extends ModelRepository[Event, CuteguardEvent]:
  override protected val table: Fragment = fr"events"

  override protected val columns: List[String] = List(
    "receiver_user_id",
    "issuer_user_id",
    "action",
    "amount",
    "date",
  )

  override def toModel(event: Event): Maybe[CuteguardEvent] =
    for
      receiver <- EitherT.liftF(users.findByID(event.receiverUserId).value)
      issuer   <- EitherT.liftF(event.issuerUserId.traverse(users.findByID).value)
    yield CuteguardEvent(
      event.id,
      receiver,
      issuer,
      event.action,
      event.amount,
      event.date,
    )

  def add(
    user: DiscordUser,
    giver: Option[DiscordUser],
    action: Action,
    amount: Int,
    date: Option[LocalDate],
  ): IO[CuteguardEvent] =
    for
      receiver <- users.add(user)
      issuer   <- giver.traverse(users.add)
      instant   = date.fold(Instant.now)(_.atStartOfDayUTC)
      event    <- insertOne((receiver.id, issuer.map(_.id), action, amount, instant))(columns*)
    yield event

  def list(
    user: Option[DiscordUser],
    giver: Option[DiscordUser],
    action: Option[Action],
    lastDays: Option[Int],
  ): IO[List[CuteguardEvent]] =
    (for
      receiver    <- user.traverse(user => users.findByDiscordID(user.discordID))
      issuer      <- giver.traverse(giver => users.findByDiscordID(giver.discordID))
      earliestDate = lastDays.map(_.toLong).map(LocalDate.now.minusDays).map(_.atStartOfDayUTC)
      events      <- OptionT.liftF(
                       list(
                         receiver.map(receiver => fr"receiver_user_id = ${receiver.id}"),
                         issuer.map(issuer => fr"issuer_user_id = ${issuer.id}"),
                         action.map(action => fr"action = $action"),
                         earliestDate.map(earliestDate => fr"date >= $earliestDate"),
                       ),
                     )
    yield events).value.map(_.toList.flatten)
