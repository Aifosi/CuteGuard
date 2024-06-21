package cuteguard.commands
import cuteguard.model.DiscordID
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.typelevel.log4cats.Logger

import scala.util.Random
import scala.util.matching.Regex

object LumiPats extends TextCommand:
  override def pattern: Regex = "pats \\^w\\^".r

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    val isLumi = event.author.discordID == DiscordID(497098958536048651L)
    if isLumi && Random.nextFloat() < 0.01 then event.reply("<a:goodpup:1251227702892036189>").as(true)
    else IO.pure(true)

  override val description: String = "Pats the goodest pup"
