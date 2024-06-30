package cuteguard.commands

import cuteguard.Cooldown
import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

case class NotCute(cooldown: Cooldown) extends TextCommand with NoChannelLog:
  val n                       = "(n|🇳)"
  val o                       = "[o0\uD83C\uDDF4]"
  val t                       = "[t7\uD83C\uDDF9]"
  val a                       = "[a4\uD83C\uDDE6]"
  val c                       = "(c|\uD83C\uDDE8)"
  val u                       = "(u|\uD83C\uDDFA)"
  val i                       = "[i1\uD83C\uDDEE]"
  val e                       = "[e3\uD83C\uDDEA]"
  override def pattern: Regex =
    s"$n[^a-z]*$o[^a-z]*$t\\s*[^a-z]*$a?\\s*$c[^a-z]*$u[^a-z]*$t[^a-z]*$i?[^a-z]*$e?".r
  def uncutePattern: Regex    = s"$u[^a-z]*$n[^a-z]*$c[^a-z]*$u[^a-z]*$t[^a-z]*$i?[^a-z]*$e?".r

  override def matches(event: MessageEvent): Boolean =
    val stripped = stripAccents(event.content).toLowerCase
    val notCute  = pattern.findFirstIn(stripped).nonEmpty
    val uncute   = uncutePattern.findFirstIn(stripped).nonEmpty
    notCute || uncute

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    lazy val embed = Embed(
      s"Lies - you're cute ${event.authorName}",
      "According to server rule 1, you are cute.\nJust accept it cutie! \uD83D\uDC9C",
      "https://media.tenor.com/iESegr2Kb6MAAAAC/narpy-cute.gif",
      "created by a sneaky totally not cute kitty",
    )
    cooldown.interact(event.author)(event.reply(embed).void)

  override val description: String = "Responds when a user says they are not cute"
