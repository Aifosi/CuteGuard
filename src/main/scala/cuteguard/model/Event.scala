package cuteguard.model

import cuteguard.model.discord.Member

import java.util.UUID

case class Event(
  id: UUID,
  receiver: Option[Member],
  issuer: Option[Option[Member]],
  action: Action,
  amount: Int,
)
