package cuteguard.model

import cuteguard.model.discord.Member

import java.util.UUID

class User(
  val id: UUID,
  member: Member,
) extends Member(member.member)
