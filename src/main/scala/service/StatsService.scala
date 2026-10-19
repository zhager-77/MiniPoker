package service

import cats.syntax.all.*
import cats.effect.{Ref, Sync}
import poker.Stats

trait StatsService[F[_]]:
  def startRequest: F[Unit]
  def finishRequest: F[Unit]
  def get: F[Stats]

object StatsService:
  def make[F[_]: Sync]: F[StatsService[F]] =
    Ref.of[F, Stats](Stats(0, 0)).map { ref =>
      new StatsService[F] {
        def startRequest: F[Unit] =
          ref.update(stats => stats.copy(inFlight = stats.inFlight + 1))

        def finishRequest: F[Unit] =
          ref.update(stats =>
            stats.copy(
              totalRequests = stats.totalRequests + 1,
              inFlight = stats.inFlight - 1
            )
          )

        def get: F[Stats] =
          ref.get
      }
    }
