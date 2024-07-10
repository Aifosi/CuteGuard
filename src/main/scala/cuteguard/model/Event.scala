package cuteguard.model

import cuteguard.model.discord.Member

import java.util.UUID

case class Event(
  id: UUID,
  receiver: Member,
  issuer: Option[Member],
  action: Action,
  amount: Int,
)
