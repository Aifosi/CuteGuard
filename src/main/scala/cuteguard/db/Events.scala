package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.{Action, Event as CuteguardEvent}
import cuteguard.model.discord.User as DiscordUser
import cuteguard.utils.Maybe

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.instances.option.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.string.*

import java.util.UUID
case class Event(
  id: UUID,
  receiverUserId: UUID,
  issuerUserId: Option[UUID],
  action: Action,
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
    // action   <- EitherT.fromEither[IO](Try(Action.valueOf(event.action)).toEither)
    yield CuteguardEvent(
      event.id,
      receiver,
      issuer,
      event.action,
      event.amount,
    )

  def add(user: DiscordUser, giver: Option[DiscordUser], action: Action, amount: Int): IO[CuteguardEvent] =
    for
      receiver <- users.add(user)
      issuer   <- giver.traverse(users.add)
      event    <- insertOne((receiver.id, issuer.map(_.id), action, amount))(columns*)
    yield event

  def list(user: DiscordUser, giver: Option[DiscordUser], action: Action): IO[List[CuteguardEvent]] =
    (for
      receiver <- users.find(user.discordID.equalDiscordID)
      issuer   <- giver.traverse(giver => users.find(giver.discordID.equalDiscordID))
      events   <- OptionT.liftF(
                    list(
                      fr"receiver_user_id = ${receiver.id}".some,
                      issuer.map(issuer => fr"issuer_user_id = ${issuer.id}"),
                      fr"action = $action".some,
                    ),
                  )
    yield events).value.map(_.toList.flatten)
