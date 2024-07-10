package cuteguard.model.discord.event

import net.dv8tion.jda.api.entities.{Guild as JDAGuild, Member as JDAMember, User as JDAUser}
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

abstract class GenericTextEvent(
  jdaChannel: MessageChannel,
  jdaAuthor: JDAUser,
  jdaMember: Option[JDAMember],
  jdaGuild: Option[JDAGuild],
) extends Event(jdaChannel, jdaAuthor, jdaMember, jdaGuild):
  lazy val authorName: String = jdaMember.flatMap(m => Option(m.getEffectiveName)).getOrElse(author.accountName)
