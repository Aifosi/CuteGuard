package cuteguard.model

import cuteguard.mapping.{OptionReader, OptionWritter}

import doobie.{Get, Put}

import scala.util.Try

enum Action(pluralTransformer: String => String):
  case Orgasm   extends Action(_ + "s")
  case Edge     extends Action(_ + "s")
  case Ruin     extends Action(_ + "s")
  case WetDream extends Action(_ + "s")
  case Subsmash extends Action(_ + "es")
  case NotCute  extends Action(_ + "s")
  case Pleading extends Action(_ + "s")

  lazy val show: String   = toString.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase
  lazy val plural: String = pluralTransformer(show)

object Action:
  /*  given Read[Action] = Read.fromGet[Long]

  given Write[Action] = Write.fromPut[Long]*/

  given Get[Action] = Get[String].temap(action => Try(Action.valueOf(action)).toEither.left.map(_.getMessage))

  given Put[Action] = Put[String].tcontramap(_.toString)

  def fromString(string: String): Either[String, Action] =
    Try(Action.valueOf(string.split(" ").map(_.capitalize).mkString)).toOption
      .toRight(s"Invalid Action `$string` please use one of ${Action.values.map(_.show).mkString(", ")}.")

  given OptionReader[Action] = OptionReader[String].emap(fromString)

  given OptionWritter[Action] = OptionWritter[String].contramap(_.show)
