package cuteguard.commands

import cuteguard.{Fitness, SubsmashConfiguration}
import cuteguard.Cooldown
import cuteguard.Fitness.sanitise
import cuteguard.db.Preferences
import cuteguard.model.Action
import cuteguard.model.discord.{Discord, Embed, Guild}
import cuteguard.model.discord.event.MessageEvent

import cats.effect.*
import cats.instances.option.*
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

case class Subsmash(
  cooldown: Cooldown,
  preferences: Preferences,
  fitness: Fitness,
  discord: Deferred[IO, Discord],
  config: SubsmashConfiguration,
  link: String,
) extends TextCommand with NoChannelLog:
  import fitness.*
  override def pattern: Regex = ".*".r

  private def resetActivity(name: String) = for
    _             <- IO.sleep(config.activityReset)
    discord       <- discord.get
    maybeActivity <- discord.activity
    _             <- maybeActivity.fold(IO.unit)(currentName => IO.whenA(currentName == name)(discord.clearActivity))
  yield ()

  private def sendReply(event: MessageEvent) = for
    discord     <- discord.get
    activityName = s"Tormenting ${event.authorName}"
    _           <- discord.activity(activityName).attempt
    _           <- resetActivity(activityName).start
    embed        = Embed(
                     s"${event.authorName}, use your words cutie",
                     link,
                   )
    _           <- event.reply(embed)
  yield ()

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    val members     = Subsmash.memberNames(event.guild, event.authorName)
    val interaction = Subsmash.best(fitness, config)(event.content, members).flatMap {
      case Some((_, _, fitnessScore)) if fitnessScore > config.threshold => sendReply(event).void
      case _                                                             => IO.unit
    }
    for
      optedOut <- preferences.find(event.author, Some("subsmash")).fold(false)(_.notCuteOptOut)
      _        <- cooldown.interact(event.author)(Action.NotCute, IO.unlessA(optedOut)(interaction))
    yield true

  override val description: String = "Responds when a user says they are not cute"

object Subsmash:
  def memberNames(guild: IO[Guild], author: String): IO[Set[String]] =
    for
      guild      <- guild
      members    <- guild.members.compile.toList
      memberNames = members.flatMap(_.guildName.sanitise.split(" ")).toSet.filter(_.length >= 4) - author.sanitise
    yield memberNames

  def best(
    fitness: Fitness,
    config: SubsmashConfiguration,
  )(
    messageText: String,
    memberNames: IO[Set[String]],
  ): IO[Option[(String, String, Double)]] =
    if messageText.length < config.minLength then return IO.pure(None)
    import fitness.*
    for
      memberNames  <- memberNames
      wordAndScore <- memberNames
                        .foldLeft(messageText)((filteredText, name) => filteredText.replace(name, ""))
                        .replaceAll(" +", " ")
                        .maxWordFitness(config.minLength)
    yield Some(wordAndScore)
