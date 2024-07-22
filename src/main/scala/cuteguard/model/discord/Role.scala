package cuteguard.model.discord

import cuteguard.mapping.{OptionReader, OptionWritter}

import cats.Show
import net.dv8tion.jda.api.entities.Role as JDARole

import scala.compiletime.asMatchable

class Role(private[model] val role: JDARole):
  lazy val discordID: DiscordID = role.getIdLong
  lazy val name: String         = role.getName
  lazy val mention: String      = role.getAsMention

  override def toString: String = name

  override def equals(other: Any): Boolean = other.asMatchable match
    case that: Role => discordID == that.discordID
    case _          => false

  override def hashCode(): Int =
    val state = Seq(discordID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)

object Role:
  given Show[Role] = Show.fromToString

  given OptionReader[Role]  = OptionReader.shouldNeverBeUsed("Role")
  given OptionWritter[Role] = OptionWritter.shouldNeverBeUsed("Role")
