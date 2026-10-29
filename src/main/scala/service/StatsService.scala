package service

import cats.syntax.all.*
import cats.effect.{Ref, Sync}
import poker.Stats

import scala.concurrent.duration.FiniteDuration

trait StatsService[F[_]]:
  def startRequest: F[Unit]
  def finishRequest(duration: FiniteDuration): F[Unit]
  def get: F[Stats]
  def reject: F[Unit]

object StatsService:
  def make[F[_]: Sync]: F[StatsService[F]] =
    Ref.of[F, Stats](Stats(0, 0, 0, 0)).map { ref =>
      new StatsService[F] {
        def startRequest: F[Unit] =
          ref.update(stats => stats.copy(inFlight = stats.inFlight + 1))

        def finishRequest(duration: FiniteDuration): F[Unit] =
          ref.update(stats =>
            stats.copy(
              totalRequests = stats.totalRequests + 1,
              inFlight = stats.inFlight - 1,
              totalProcessingMs = stats.totalProcessingMs + duration.toMillis
            )
          )

        def get: F[Stats] =
          ref.get

        def reject: F[Unit] =
          ref.update(rejects =>
            rejects.copy(rejectedRequests = rejects.rejectedRequests + 1)
          )
      }
    }
