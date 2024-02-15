package cuteguard.model

import java.time.Instant
import java.util.UUID

class CuteGuardUser(
  val id: UUID,
  member: Member,
  val points: Int,
  val pointsToday: Int,
  val pointsLastUpdate: Instant,
) extends Member(member.member):
  override lazy val toString: String = member.toString
