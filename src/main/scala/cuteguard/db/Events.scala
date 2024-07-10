package cuteguard.db

import cuteguard.model.{Action, Event as CuteguardEvent}
import cuteguard.model.discord.{DiscordID, Guild}
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
  receiverUserId: DiscordID,
  issuerUserId: Option[DiscordID],
  action: String,
  amount: Int,
)

class Events(guild: Guild)(using Transactor[IO]) extends ModelRepository[Event, CuteguardEvent]:
  override protected val table: Fragment = fr"events"

  override protected val columns: List[String] = List(
    "id",
    "receiver_user_id",
    "issuer_user_id",
    "action",
    "amount",
  )

  override def toModel(event: Event): Maybe[CuteguardEvent] =
    for
      receiver <- guild.member(event.receiverUserId)
      issuer   <- event.issuerUserId.traverse(guild.member)
      action   <- EitherT.fromEither[IO](Try(Action.valueOf(event.action)).toEither)
    yield CuteguardEvent(
      event.id,
      receiver,
      issuer,
      action,
      event.amount,
    )
