package cuteguard.commands

import cuteguard.{Fitness, SubsmashConfiguration}
import cuteguard.Cooldown
import cuteguard.model.Action
import cuteguard.model.discord.{Discord, Embed, Guild}
import cuteguard.model.discord.event.MessageEvent

import cats.effect.*
import cats.instances.option.*
import org.apache.commons.lang3.StringUtils.stripAccents
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
    Subsmash.best(fitness, config)(event.content, event.guild).flatMap {
      case Some((word, fitnessScore)) if fitnessScore > config.threshold =>
        cooldown.interact(event.author)(Action.Subsmash, sendReply(event))
      case _                                                             => IO.pure(true)
    }

  override val description: String = "Responds when a user says they are not cute"

object Subsmash:
  extension (string: String)
    def sanitise: String =
      stripAccents(string)                          // Remove diacritics
        .toLowerCase                                // to lowercase
        .replaceAll("https?://[^ ]+\\.[^ ]+", "")   // remove links
        .replaceAll("<a?:\\w+:\\d+>", "")           // remove emoji and links
        .replaceAll("`(:?``)?[^`]+`(:?``)?", "")    // remove code blocks
        .replaceAll("(\\w+)[^\\w ](\\w+)", "$1 $2") // remove word alternations
        .replaceAll("(\\w+)\\1+", "$1")             // remove single character repetitions
        .replaceAll("[^a-z \n]", "")                // Remove all symbols

  def best(fitness: Fitness, config: SubsmashConfiguration)(
    messageText: String,
    guild: IO[Guild],
  ): IO[Option[(String, Double)]] =
    val sanitized = messageText.sanitise
    if sanitized.length < config.minLength then return IO.pure(None)

    import fitness.*

    for
      guild      <- guild
      members    <- guild.members.compile.toList
      memberNames = members.flatMap(_.guildName.sanitise.split(" ")).toSet.filter(_.length >= 4)

      (word, fitnessScore) <- memberNames
                                .foldLeft(sanitized)((filteredText, name) => filteredText.replace(name, ""))
                                .replace("  ", " ")
                                .maxWordFitness(config.minLength)
    yield Some(word -> fitnessScore)
