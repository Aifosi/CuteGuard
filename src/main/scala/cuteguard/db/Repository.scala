package cuteguard.db

import cats.data.OptionT
import cats.syntax.option.*
import doobie.*
import doobie.implicits.*
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
  def combineFilters: Fragment                                                                              = mkFragment(fr"where", fr"and")

trait RepositoryFields:
  protected val table: Fragment
  protected val allColumns: List[String]

trait Insert[DB: Read]:
  this: RepositoryFields =>
  protected val table: Fragment

  protected def unknowns(columns: Int): String = List.fill(columns)("?").mkString("(", ", ", ")")

  protected def sql(columns: String*): String =
    fr"insert into $table".internals.sql + columns.mkString("(", ", ", ") values") + unknowns(columns.length)

  def insertOne[Info: Write](info: Info)(columns: String*): ConnectionIO[DB] =
    Update[Info](sql(columns*)).withUniqueGeneratedKeys[DB](this.allColumns*)(info)

  def insertMany[Info: Write](info: List[Info])(columns: String*): Stream[ConnectionIO, DB] =
    Update[Info](sql(columns*)).updateManyWithGeneratedKeys[DB](this.allColumns*)(info)

trait Remove[DB: Read]:
  this: Repository[DB] =>
  protected val table: Fragment

  def remove(filter: Filter, moreFilters: Filter*): ConnectionIO[Int] =
    (fr"delete from" ++ table ++ (filter +: moreFilters).toList.combineFilters ++ fr"").update.run

trait Repository[DB: Read] extends RepositoryFields with Insert[DB] with Remove[DB]:
  private val updatedAt: Filter = fr"updated_at = NOW()".some

  private inline def updateQuery(updates: Filter*)(where: Fragment, more: Fragment*) =
    (updates.toList :+ updatedAt)
      .mkFragment(fr"update $table set", fr",", (where +: more.toList).map(_.some).combineFilters)
      .update

  protected inline def innerUpdateMany(updates: Filter*)(where: Fragment, more: Fragment*): ConnectionIO[List[DB]] =
    updateQuery(updates*)(where, more*)
      .withGeneratedKeys[DB](allColumns*)
      .compile
      .toList

  protected inline def innerUpdate(updates: Filter*)(where: Fragment, more: Fragment*): ConnectionIO[DB] =
    updateQuery(updates*)(where, more*)
      .withUniqueGeneratedKeys[DB](allColumns*)

  protected lazy val selectAll: Fragment = Fragment.const(allColumns.mkString("select ", ", ", " from")) ++ table

  private def query(filters: Iterable[Filter]) =
    (selectAll ++ filters.toList.combineFilters).query[DB]

  def list(filters: Filter*): ConnectionIO[List[DB]] = query(filters).to[List]

  def find(filters: Filter*): OptionT[ConnectionIO, DB] = OptionT(query(filters).option)

  def get(filters: Filter*): ConnectionIO[DB] =
    find(filters*).getOrElseF(FC.raiseError(new Exception("Failed to find item in repository")))

  def update(updates: Filter*)(where: Fragment, more: Fragment*): ConnectionIO[DB] = innerUpdate(updates*)(where, more*)

  def updateMany(updates: Filter*)(where: Fragment, more: Fragment*): ConnectionIO[List[DB]] =
    innerUpdateMany(updates*)(where, more*)