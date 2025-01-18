package cuteguard

import cats.effect.IO
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import pureconfig.ConfigReader

final case class PostgresConfiguration(
  driver: String,
  user: String,
  password: String,
  url: String,
) derives ConfigReader:
  def transactor(using logHandler: LogHandler[IO]): Transactor[IO] = Transactor.fromDriverManager[IO](
    driver,
    url,
    user,
    password,
    Some(logHandler),
  )
