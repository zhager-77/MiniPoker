package api

import cats.effect.Async
import cats.effect.std.Semaphore
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import dto.PokerDtos.*
import dto.Converter.*
import service.{PokerService, StatsService}
import poker.Ids.PlayerId
import cats.effect.syntax.all.*

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

object PokerRoutes:
  def make[F[_]: Async](
      pokerService: PokerService[F],
      statsService: StatsService[F],
      semaphore: Semaphore[F],
      acquireTimeout: FiniteDuration
  ): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "poker" / "stats" =>
        statsService.get.flatMap(stats =>
          Ok(StatsDto(stats.totalRequests, stats.inFlight))
        )
      case GET -> Root / "poker" / "best-hand" / playerId =>
        pokerService.bestHand(PlayerId(playerId)).flatMap {
          case Some(hand) => Ok(fromHand(hand))
          case None       => NotFound(StatusMessage("Player not found"))
        }
      // Запрос, который получил 503, не попадает в статистику. attemptAs вместо as, кривой json падает 400, а не 500
      case req @ POST -> Root / "poker" / "evaluate-players" =>
        semaphore.acquire.timeout(acquireTimeout).attempt.flatMap {
          case Left(_: TimeoutException) =>
            ServiceUnavailable(
              StatusMessage("Too many concurrent requests, please retry later")
            )
          case Left(other) => other.raiseError[F, Response[F]]
          case Right(_) =>
            val handle: F[Response[F]] =
              req
                .attemptAs[List[UserHandDto]]
                .value
                .flatMap {
                  case Left(failure) =>
                    BadRequest(
                      StatusMessage(s"Failed request: ${failure.message}")
                    )
                  case Right(body) =>
                    body.traverse(toPlayer) match
                      case Left(error) =>
                        ErrorHandling.toHttpResponse[F](error).pure[F]
                      case Right(players) =>
                        pokerService.evaluatePlayers(players).flatMap {
                          case Left(error) =>
                            ErrorHandling.toHttpResponse[F](error).pure[F]
                          case Right(result) =>
                            Ok(
                              EvaluatePlayersDto(
                                result.winners.map(fromPlayer),
                                result.losers.map(fromPlayer)
                              )
                            )
                        }
                }
            (statsService.startRequest *> handle.guarantee(statsService.finishRequest))
              .guarantee(semaphore.release)
        }
    }
