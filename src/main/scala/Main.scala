import cats.effect.{IO, IOApp}
import cats.effect.std.Semaphore
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import pureconfig.ConfigSource
import api.PokerRoutes
import config.AppConfig
import service.{PlayerStore, PokerService, StatsService}
import doobie.util.transactor.Transactor
import db.HistoryRepository

object Main extends IOApp.Simple:

  def run: IO[Unit] =
    for
      config <- IO(ConfigSource.default.loadOrThrow[AppConfig])
      host <- IO.fromOption(Host.fromString(config.server.host))(
        new RuntimeException(s"Invalid host: ${config.server.host}")
      )
      port <- IO.fromOption(Port.fromInt(config.server.port))(
        new RuntimeException(s"Invalid port: ${config.server.port}")
      )
      store <- PlayerStore.make[IO]
      stats <- StatsService.make[IO]
      xa = Transactor.fromDriverManager[IO](
        driver = "org.h2.Driver",
        url = config.db.url,
        user = config.db.user,
        password = config.db.password,
        logHandler = None
      )
      history <- HistoryRepository.make[IO](xa)
      poker = PokerService.make[IO](store, history)
      sem <- Semaphore[IO](config.poker.maxConcurrentRequests)
      routes = PokerRoutes
        .make[IO](poker, stats, sem, config.poker.acquireTimeout)
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(routes.orNotFound)
        .build
        .useForever
    yield ()
