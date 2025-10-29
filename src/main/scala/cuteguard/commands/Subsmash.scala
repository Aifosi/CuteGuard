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
) extends TextCommand with NoChannelLog with Hidden:
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
    val members = Subsmash.memberNames(event.guild, event.authorName)
    List(
      Subsmash.best(fitness, config)(event.content, members).map(_.exists(_(2) > config.threshold)),
      cooldown.addEventAndCheckReady(event.author, Action.NotCute),
      preferences.find(event.author, label = Some("subsmash")).fold(true)(!_.subsmashOptOut),
    ).foldLeft(IO.pure(true)) { case (acc, io) =>
      acc.flatMap {
        case false => IO.pure(false)
        case true  => io
      }
    }.flatMap(IO.whenA(_)(sendReply(event)))
      .start
      .as(true)

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
