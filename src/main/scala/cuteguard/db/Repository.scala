package cuteguard.db

import cats.data.OptionT
import cats.syntax.option.*
import doobie.*
import doobie.implicits.*
import doobie.util.unlabeled
import fs2.Stream

import scala.concurrent.duration.*

given Get[FiniteDuration] = Get[Long].map(_.seconds)
given Put[FiniteDuration] = Put[Long].contramap(_.toSeconds)

type Filter = Option[Fragment]

extension (filters: List[Filter])
  def mkFragment(start: Fragment = Fragment.empty, sep: Fragment, end: Fragment = Fragment.empty): Fragment =
    val flattened = filters.flatten
    if flattened.isEmpty then Fragment.empty
    else start ++ flattened.reduceLeft(_ ++ sep ++ _) ++ end

  def combineFilters: Fragment = mkFragment(fr"where", fr"and")

trait RepositoryFields:
  protected val table: Fragment
  protected lazy val allColumns: List[String]

trait Insert[DB: Read]:
  this: RepositoryFields =>
  protected val table: Fragment

  protected def unknowns(columns: Int): String = List.fill(columns)("?").mkString("(", ", ", ")")

  protected def sql(columns: String*): String =
    fr"insert into $table".internals.sql + columns.mkString("(", ", ", ") values") + unknowns(columns.length)

  def insertOne[Info: Write](info: Info)(columns: String*)(label: Option[String]): ConnectionIO[DB] =
    Update[Info](sql(columns*), None, label.getOrElse(unlabeled)).withUniqueGeneratedKeys[DB](this.allColumns*)(info)

  def insertMany[Info: Write](info: List[Info])(columns: String*)(label: Option[String]): Stream[ConnectionIO, DB] =
    Update[Info](sql(columns*), None, label.getOrElse(unlabeled))
      .updateManyWithGeneratedKeys[DB](this.allColumns*)(info)

trait Remove[DB]:
  this: Repository[DB] =>
  protected val table: Fragment

  def remove(filter: Filter, moreFilters: Filter*)(label: Option[String]): ConnectionIO[Int] =
    (fr"delete from" ++ table ++ (filter +: moreFilters).toList.combineFilters ++ fr"")
      .updateWithLabel(label.getOrElse(unlabeled))
      .run

trait Repository[DB: Read] extends RepositoryFields with Insert[DB] with Remove[DB]:
  private val updatedAt: Filter = fr"updated_at = NOW()".some

  private inline def updateQuery(updates: Filter*)(where: Fragment, more: Fragment*)(label: Option[String]) =
    (updates.toList :+ updatedAt)
      .mkFragment(fr"update $table set", fr",", (where +: more.toList).map(_.some).combineFilters)
      .updateWithLabel(label.getOrElse(unlabeled))

  protected inline def innerUpdateMany(
    updates: Filter*,
  )(where: Fragment, more: Fragment*)(label: Option[String]): ConnectionIO[List[DB]] =
    updateQuery(updates*)(where, more*)(label)
      .withGeneratedKeys[DB](allColumns*)
      .compile
      .toList

  protected inline def innerUpdate(
    updates: Filter*,
  )(where: Fragment, more: Fragment*)(label: Option[String]): ConnectionIO[DB] =
    updateQuery(updates*)(where, more*)(label)
      .withUniqueGeneratedKeys[DB](allColumns*)

  lazy val selectAll: Fragment = Fragment.const(allColumns.mkString("select ", ", ", " from")) ++ table

  private def query(filters: Iterable[Filter])(label: Option[String]) =
    (selectAll ++ filters.toList.combineFilters).queryWithLabel[DB](label.getOrElse(unlabeled))

  def list(filters: Filter*)(label: Option[String]): ConnectionIO[List[DB]] = query(filters)(label).to[List]

  def find(filters: Filter*)(label: Option[String]): OptionT[ConnectionIO, DB] = OptionT(query(filters)(label).option)

  def get(filters: Filter*)(label: Option[String]): ConnectionIO[DB] =
    find(filters*)(label).getOrElseF(FC.raiseError(new Exception("Failed to find item in repository")))

  def update(updates: Filter*)(where: Fragment, more: Fragment*)(label: Option[String]): ConnectionIO[DB] =
    innerUpdate(updates*)(where, more*)(label)

  def updateMany(updates: Filter*)(where: Fragment, more: Fragment*)(label: Option[String]): ConnectionIO[List[DB]] =
    innerUpdateMany(updates*)(where, more*)(label)
