package cuteguard.db

import cuteguard.db.Filters.*
import cuteguard.model.UserPreferences as CuteguardUserPreferences
import cuteguard.model.discord.User as DiscordUser
import cuteguard.utils.Maybe

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.{Fragment, Transactor}
import doobie.postgres.implicits.*
import doobie.syntax.SqlInterpolator.SingleFragment
import doobie.syntax.string.*

import java.util.UUID

case class UserPreferences(
  id: UUID,
  user_id: UUID,
  pleadingOptOut: Boolean,
  subsmashOptOut: Boolean,
  notCuteOptOut: Boolean,
)

class Preferences(val users: Users)(using Transactor[IO])
    extends ModelRepository[UserPreferences, CuteguardUserPreferences]:
  override protected val table: Fragment = fr"preferences"

  override protected val columns: List[String] = List(
    "user_id",
    "pleading_opt_out",
    "subsmash_opt_out",
    "not_cute_opt_out",
  )

  override def toModel(label: Option[String])(event: UserPreferences): Maybe[CuteguardUserPreferences] =
    for user <- EitherT.liftF(users.findByID(event.user_id, label).value)
    yield CuteguardUserPreferences(
      event.id,
      user,
      event.pleadingOptOut,
      event.subsmashOptOut,
      event.notCuteOptOut,
    )

  def add(
    user: DiscordUser,
    pleadingOptOut: Boolean = false,
    subsmashOptOut: Boolean = false,
    notCuteOptOut: Boolean = false,
    label: Option[String] = None,
  ): IO[CuteguardUserPreferences] =
    for
      user        <- users.findOrAdd(user, label)
      preferences <- insertOne((user.id, pleadingOptOut, subsmashOptOut, notCuteOptOut))(columns*)(label)
    yield preferences

  def find(user: DiscordUser, label: Option[String] = None): OptionT[IO, CuteguardUserPreferences] =
    super.find(user.discordID.equalDiscordID)(label)

  def update(
    id: UUID,
    pleadingOptOut: Option[Boolean] = None,
    subsmashOptOut: Option[Boolean] = None,
    notCuteOptOut: Option[Boolean] = None,
    label: Option[String] = None,
  ): IO[CuteguardUserPreferences] =
    super.update(
      pleadingOptOut.map(pleadingOptOut => fr"pleading_opt_out = $pleadingOptOut"),
      subsmashOptOut.map(subsmashOptOut => fr"subsmash_opt_out = $subsmashOptOut"),
      notCuteOptOut.map(notCuteOptOut => fr"not_cute_opt_out = $notCuteOptOut"),
    )(fr"id = $id")(label)
