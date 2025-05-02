package cuteguard.commands

import cuteguard.Cooldown
import cuteguard.db.Preferences
import cuteguard.model.Action
import cuteguard.model.discord.Embed
import cuteguard.model.discord.event.MessageEvent

import cats.effect.IO
import org.apache.commons.lang3.StringUtils.stripAccents
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

case class NotCute(cooldown: Cooldown, preferences: Preferences, link: String) extends TextCommand with NoChannelLog:
  val n = "(?:n|🇳)"
  val o = "[o0\uD83C\uDDF4]"
  val t = "[t7\uD83C\uDDF9]"
  val a = "[a4\uD83C\uDDE6]"
  val c = "(?:c|\uD83C\uDDE8)"
  val u = "(?:u|\uD83C\uDDFA)"
  val i = "[i1\uD83C\uDDEE]"
  val e = "[e3\uD83C\uDDEA]"

  def notCutePattern: Regex =
    s"$n[^a-z]*$o[^a-z]*$t\\s*[^a-z]*$a?\\s*$c[^a-z]*$u[^a-z]*$t[^a-z]*$i?[^a-z]*$e".r
  def uncutePattern: Regex  = s"$u[^a-z]*$n[^a-z]*$c[^a-z]*$u[^a-z]*$t[^a-z]*$i?[^a-z]*$e?".r

  override def pattern: Regex = s"\\b($notCutePattern|$uncutePattern)\\b".r

  override def matches(event: MessageEvent): Boolean =
    val stripped = stripAccents(event.content).toLowerCase
    pattern.findFirstIn(stripped).nonEmpty

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    lazy val embed = Embed(
      s"Lies - you're cute ${event.authorName}",
      "According to server rule 1, you are cute.\nJust accept it cutie! \uD83D\uDC9C",
      link,
    )

    List(
      cooldown.addEventAndCheckReady(event.author, Action.NotCute),
      preferences.find(event.author, Some("not cute")).fold(false)(_.notCuteOptOut),
    ).foldLeft(IO.pure(true)) { case (acc, io) =>
      acc.flatMap {
        case false => IO.pure(false)
        case true  => io
      }
    }.flatMap(IO.whenA(_)(event.reply(embed).void))
      .start
      .as(true)

  override val description: String = "Responds when a user says they are not cute"
