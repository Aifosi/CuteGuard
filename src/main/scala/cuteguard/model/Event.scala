package cuteguard.model

import java.time.Instant
import java.util.UUID

case class Event(
  id: UUID,
  receiver: Option[User],
  issuer: Option[Option[User]],
  action: Action,
  amount: Int,
  date: Instant,
)
