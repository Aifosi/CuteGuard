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
  def thoroughList(filters: Filter*)(label: Option[String]): IO[List[Either[ID, Model]]] =
    Repo
      .list(filters*)(label)
      .transact(this.transactor)
      .flatMap(_.traverse(dbModel => toModel(label)(dbModel).leftMap(_ => id(dbModel)).value))

trait ModelRepository[DB: Read, Model](using val transactor: Transactor[IO]) extends RepositoryFields:
  outer =>
  def toModel(label: Option[String])(a: DB): Maybe[Model]
  protected val columns: List[String]

  final override protected lazy val allColumns: List[String]       = "id" +: columns
  final def unsafeToModel(label: Option[String])(a: DB): IO[Model] = toModel(label)(a).rethrowT

  private[db] object Repo extends Repository[DB]:
    override protected val table: Fragment               = outer.table
    override protected lazy val allColumns: List[String] = outer.allColumns

  private inline def toModelList(label: Option[String])(dbModel: DB): IO[List[Model]] =
    toModel(label)(dbModel).value.map(_.toSeq.toList)

  protected def list(filters: Filter*)(label: Option[String]): IO[List[Model]] =
    Repo.list(filters*)(label).transact(transactor).flatMap(_.flatTraverse(toModelList(label)))

  protected def find(filters: Filter*)(label: Option[String]): OptionT[IO, Model] =
    Repo.find(filters*)(label).transact(transactor).flatMap(toModel(label)(_).toOption)

  protected def get(filters: Filter*)(label: Option[String]): IO[Model] =
    Repo.get(filters*)(label).transact(transactor).flatMap(unsafeToModel(label))

  protected def update(updates: Filter*)(where: Fragment, more: Fragment*)(label: Option[String]): IO[Model] =
    Repo.update(updates*)(where, more*)(label).transact(transactor).flatMap(unsafeToModel(label))

  protected def updateMany(updates: Filter*)(where: Fragment, more: Fragment*)(label: Option[String]): IO[List[Model]] =
    Repo.updateMany(updates*)(where, more*)(label).transact(transactor).flatMap(_.traverse(unsafeToModel(label)))

  protected def remove(filter: Filter, moreFilters: Filter*)(label: Option[String]): IO[Int] =
    Repo.remove(filter, moreFilters*)(label).transact(transactor)

  protected def insertOne[Info: Write](info: Info)(columns: String*)(label: Option[String]): IO[Model] =
    Repo.insertOne(info)(columns*)(label).transact(transactor).flatMap(unsafeToModel(label))

  /*def insertMany[Info: Write](info: List[Info])(columns: String*): Stream[IO, DB] =
    Repo.insertMany(info)(columns*).transact(transactor)*/
