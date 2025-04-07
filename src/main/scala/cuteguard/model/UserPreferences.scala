package cuteguard.model

import java.util.UUID

case class UserPreferences(
  id: UUID,
  user: Option[User],
  pleadingOptOut: Boolean,
  subsmashOptOut: Boolean,
  notCuteOptOut: Boolean,
)
