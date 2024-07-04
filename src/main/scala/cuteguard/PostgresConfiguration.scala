package cuteguard

import cats.effect.IO
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import pureconfig.*
import pureconfig.generic.derivation.default.derived

final case class PostgresConfiguration(
  driver: String,
  user: String,
  password: String,
  url: String,
) derives ConfigReader:
  def transactor(logHandler: LogHandler[IO]): Transactor[IO] = Transactor.fromDriverManager[IO](
    driver,
    url,
    user,
    password,
    Some(logHandler),
  )
