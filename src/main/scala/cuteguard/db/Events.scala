package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.{Action, Event as CuteguardEvent}
import cuteguard.model.discord.User as DiscordUser
import cuteguard.utils.Maybe

import cats.data.EitherT
import cats.effect.IO
import cats.instances.option.*
import cats.syntax.traverse.*
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.string.*

import java.util.UUID
import scala.util.Try
case class Event(
  id: UUID,
  receiverUserId: UUID,
  issuerUserId: Option[UUID],
  action: String,
  amount: Int,
)

class Events(users: Users)(using Transactor[IO]) extends ModelRepository[Event, CuteguardEvent]:
  override protected val table: Fragment = fr"events"

  override protected val columns: List[String] = List(
    "receiver_user_id",
    "issuer_user_id",
    "action",
    "amount",
  )

  override def toModel(event: Event): Maybe[CuteguardEvent] =
    for
      receiver <- EitherT.liftF(users.get(event.receiverUserId.equalID))
      issuer   <- EitherT.liftF(event.issuerUserId.traverse(issuerUserId => users.get(issuerUserId.equalID)))
      action   <- EitherT.fromEither[IO](Try(Action.valueOf(event.action)).toEither)
    yield CuteguardEvent(
      event.id,
      receiver,
      issuer,
      action,
      event.amount,
    )

  def add(user: DiscordUser, giver: Option[DiscordUser], action: Action, amount: Int): IO[CuteguardEvent] =
    for
      receiver <- users.add(user)
      issuer   <- giver.traverse(users.add)
      event    <- insertOne((receiver.id, issuer.map(_.id), action.toString, amount))(columns*)
    yield event
