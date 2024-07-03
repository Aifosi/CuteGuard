package cuteguard.commands

import cuteguard.{Fitness, SubsmashConfiguration}
import cuteguard.Cooldown
import cuteguard.model.Discord
import cuteguard.model.Embed
import cuteguard.model.event.MessageEvent

import cats.effect.*
import cats.instances.option.*
import org.typelevel.log4cats.Logger

import scala.util.matching.Regex

case class Subsmash(cooldown: Cooldown, fitness: Fitness, discord: Deferred[IO, Discord], config: SubsmashConfiguration)
    extends TextCommand with NoChannelLog:
  import fitness.*
  override def pattern: Regex = ".*".r

  private def resetActivity(name: String) = for
    _             <- IO.sleep(config.activityReset)
    discord       <- discord.get
    maybeActivity <- discord.activity
    _             <- maybeActivity.fold(IO.unit)(currentName => IO.whenA(currentName == name)(discord.clearActivity))
  yield ()

  private def matches(messageText: String, memberNames: Set[String])(using Logger[IO]): IO[Boolean] =
    if messageText.length < config.minLength then return IO.pure(false)

    for
      (word, quadgramsWordFitness) <- memberNames
                                        .foldLeft(messageText)((filteredText, name) => filteredText.replace(name, ""))
                                        .replace("  ", " ")
                                        .minWordFitness(config.minLength)
      matches                      <-
        if quadgramsWordFitness > config.threshold then
          Logger[IO].debug(s"word: $word, Fitness: $quadgramsWordFitness").as(true)
        else IO.pure(false)
    yield matches

  private def sendReply(event: MessageEvent) = for
    discord     <- discord.get
    activityName = s"Tormenting ${event.authorName}"
    _           <- discord.activity(activityName).attempt
    _           <- resetActivity(activityName).start
    embed        = Embed(
                     s"${event.authorName}, use your words cutie",
                     "https://cdn.discordapp.com/attachments/988232177265291324/1253319448954277949/nobottom.webp",
                     "created by a sneaky totally not cute kitty",
                   )
    _           <- event.reply(embed)
  yield ()

  override def apply(pattern: Regex, event: MessageEvent)(using Logger[IO]): IO[Boolean] =
    for
      guild      <- event.guild
      members    <- guild.members.compile.toList
      memberNames = members.flatMap(_.guildName.sanitise.split(" ")).toSet.filter(_.length >= 4)
      matches    <- matches(event.content.sanitise, memberNames)
      continue   <- if matches then cooldown.interact(event.author)(sendReply(event)) else IO.pure(true)
    yield continue

  override val description: String = "Responds when a user says they are not cute"
