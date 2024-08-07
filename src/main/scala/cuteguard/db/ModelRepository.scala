package cuteguard.db

import cuteguard.utils.Maybe

import cats.data.OptionT
import cats.effect.IO
import cats.syntax.traverse.*
import doobie.*
import doobie.implicits.*
import doobie.util.Read

trait ThoroughList[DB: Read, Model, ID]:
  this: ModelRepository[DB, Model] =>
  def id(db: DB): ID
  def thoroughList(filters: Filter*): IO[List[Either[ID, Model]]] =
    Repo
      .list(filters*)
      .transact(this.transactor)
      .flatMap(_.traverse(dbModel => toModel(dbModel).leftMap(_ => id(dbModel)).value))

trait ModelRepository[DB: Read, Model](using val transactor: Transactor[IO]) extends RepositoryFields:
  outer =>
  def toModel(a: DB): Maybe[Model]
  protected val columns: List[String]

  final override protected lazy val allColumns: List[String] = "id" +: columns
  final def unsafeToModel(a: DB): IO[Model]                  = toModel(a).rethrowT

  private[db] object Repo extends Repository[DB]:
    override protected val table: Fragment               = outer.table
    override protected lazy val allColumns: List[String] = outer.allColumns

  private inline def toModelList(dbModel: DB): IO[List[Model]] = toModel(dbModel).value.map(_.toSeq.toList)

  protected def list(filters: Filter*): IO[List[Model]] =
    Repo.list(filters*).transact(transactor).flatMap(_.flatTraverse(toModelList))

  protected def find(filters: Filter*): OptionT[IO, Model] =
    Repo.find(filters*).transact(transactor).flatMap(toModel(_).toOption)

  protected def get(filters: Filter*): IO[Model] =
    Repo.get(filters*).transact(transactor).flatMap(unsafeToModel)

  protected def update(updates: Filter*)(where: Fragment, more: Fragment*): IO[Model] =
    Repo.update(updates*)(where, more*).transact(transactor).flatMap(unsafeToModel)

  protected def updateMany(updates: Filter*)(where: Fragment, more: Fragment*): IO[List[Model]] =
    Repo.updateMany(updates*)(where, more*).transact(transactor).flatMap(_.traverse(unsafeToModel))

  protected def remove(filter: Filter, moreFilters: Filter*): IO[Int] =
    Repo.remove(filter, moreFilters*).transact(transactor)

  protected def insertOne[Info: Write](info: Info)(columns: String*): IO[Model] =
    Repo.insertOne(info)(columns*).transact(transactor).flatMap(unsafeToModel)

  /*def insertMany[Info: Write](info: List[Info])(columns: String*): Stream[IO, DB] =
    Repo.insertMany(info)(columns*).transact(transactor)*/
