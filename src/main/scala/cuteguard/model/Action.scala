package cuteguard.model

import cuteguard.mapping.{OptionReader, OptionWriter}

import cats.syntax.either.*
import doobie.{Get, Put}

import scala.util.Try

sealed trait CanOpt:
  this: Action =>

object CanOpt:
  lazy val values: Array[Action & CanOpt] = Action.values.collect { case canOpt: CanOpt =>
    canOpt
  }

  given OptionReader[Action & CanOpt] = OptionReader[Action].emap {
    case canOpt: CanOpt => canOpt.asRight
    case action         => s"Invalid Action ${action.show}".asLeft
  }

  given OptionWriter[Action & CanOpt] = OptionWriter[Action].asInstanceOf[OptionWriter[Action & CanOpt]]

enum Action(pluralTransformer: String => String):
  case Orgasm   extends Action(_ + "s")
  case Edge     extends Action(_ + "s")
  case Ruin     extends Action(_ + "s")
  case WetDream extends Action(_ + "s")
  case Spank    extends Action(_ + "s")
  case Subsmash extends Action(_ + "es") with CanOpt
  case NotCute  extends Action(_ + "s") with CanOpt
  case Pleading extends Action(_ + "s") with CanOpt

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

  given OptionWriter[Action] = OptionWriter[String].contramap(_.show)
