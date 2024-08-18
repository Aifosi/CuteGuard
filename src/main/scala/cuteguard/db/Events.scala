package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.{Action, Event as CuteguardEvent, User as CuteguardUser}
import cuteguard.model.discord.{DiscordID, User as DiscordUser}
import cuteguard.syntax.localdate.*
import cuteguard.utils.Maybe

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.instances.list.*
import cats.instances.option.*
import cats.syntax.traverse.*
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.connectionio.*
import doobie.syntax.string.*
import doobie.util.unlabeled

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

  override def toModel(label: Option[String])(event: Event): Maybe[CuteguardEvent] =
    for
      receiver <- EitherT.liftF(users.findByID(event.receiverUserId, label).value)
      issuer   <- EitherT.liftF(event.issuerUserId.traverse(users.findByID(_, label)).value)
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
    label: Option[String] = None,
  ): IO[CuteguardEvent] =
    for
      receiver <- users.add(user, label)
      issuer   <- giver.traverse(users.add(_, label))
      instant   = date.fold(Instant.now)(_.atStartOfDayUTC)
      event    <- insertOne((receiver.id, issuer.map(_.id), action, amount, instant))(columns*)(label)
    yield event

  def list(
    user: Option[DiscordUser],
    giver: Option[DiscordUser],
    action: Option[Action],
    lastDays: Option[Int],
    label: Option[String] = None,
  ): IO[List[CuteguardEvent]] =
    val earliestDate = lastDays.map(_.toLong).map(LocalDate.now.minusDays).map(_.atStartOfDayUTC)
    val filters      = List(
      user.map(user => fr"receiver_user_ids.user_discord_id = ${user.discordID}"),
      giver.map(giver => fr"issuer_user_ids.user_discord_id = ${giver.discordID}"),
      action.map(action => fr"events.action = $action"),
      earliestDate.map(earliestDate => fr"events.date >= $earliestDate"),
    )
    val columns      = List(
      "events.id",
      "events.receiver_user_id AS receiver_user_uuid",
      "receiver_user_ids.user_discord_id AS receiver_user_id",
      "events.issuer_user_id AS issuer_user_uuid",
      "issuer_user_ids.user_discord_id AS issuer_user_id",
      "events.action",
      "events.amount",
      "events.date",
    )
    val query        = Fragment.const(columns.mkString("select ", ", ", " from")) ++
      table ++
      fr"LEFT JOIN users as receiver_user_ids ON events.receiver_user_id = receiver_user_ids.id" ++
      fr"LEFT JOIN users AS issuer_user_ids ON events.issuer_user_id = issuer_user_ids.id" ++
      filters.combineFilters

    val list = query
      .queryWithLabel[(UUID, UUID, DiscordID, Option[UUID], Option[DiscordID], Action, Int, Instant)](
        label.getOrElse(unlabeled),
      )
      .to[List]
      .transact(transactor)

    (for
      list         <- EitherT.liftF(list)
      guild        <- users.guild
      discordIdsMap = list
                        .flatMap((_, receiver_user_uuid, receiver_user_id, issuer_user_uuid, issuer_user_id, _, _, _) =>
                          List((receiver_user_id, receiver_user_uuid)) ++ issuer_user_id.zip(issuer_user_uuid),
                        )
                        .toMap

      userMap <- guild
                   .members(discordIdsMap.keys.toList)
                   .map(_.map { member =>
                     val uuid = discordIdsMap(member.discordID)
                     uuid -> CuteguardUser(uuid, member)
                   }.toMap)

      events = list.map { (id, receiver_user_uuid, _, issuer_user_uuid, _, action, amount, date) =>
                 CuteguardEvent(
                   id,
                   userMap.get(receiver_user_uuid),
                   issuer_user_uuid.map(userMap.get),
                   action,
                   amount,
                   date,
                 )
               }
    yield events).toOption.value.map(_.toList.flatten)

  def delete(id: UUID, label: Option[String] = None): IO[Unit] = remove(id.equalID)(label).void

  def edit(
    id: UUID,
    giver: Option[DiscordUser],
    amount: Option[Int],
    date: Option[LocalDate],
    label: Option[String] = None,
  ): OptionT[IO, CuteguardEvent] =
    for
      giver <- giver.traverse(giver => users.findByDiscordID(giver.discordID))
      event <- OptionT.liftF(
                 update(
                   giver.map(giver => fr"issuer_user_id = ${giver.id}"),
                   amount.map(amount => fr"amount = $amount"),
                   date.map(date => fr"date = ${date.atStartOfDayUTC}"),
                 )(fr"id = $id")(label),
               )
    yield event
