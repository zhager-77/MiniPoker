package service

import cats.syntax.all.*
import cats.effect.Sync
import logic.Logic
import poker.Ids.PlayerId
import poker.{Hand, Player, PokerError, Result}
import cats.effect.std.UUIDGen
import db.HistoryRepository

trait PokerService[F[_]]:
  def evaluatePlayers(players: List[Player]): F[Either[PokerError, Result]]
  def bestHand(playerId: PlayerId): F[Option[Hand]]

//evaluatePlayers сохраняем только при успехе. Сначала БД, потом Ref, так как при сбое БД best-hand покажет руку, которой нет
object PokerService:
  def make[F[_]: Sync](
      store: PlayerStore[F],
      history: HistoryRepository[F]
  ): PokerService[F] =
    new PokerService[F]:
      def evaluatePlayers(
          players: List[Player]
      ): F[Either[PokerError, Result]] =
        Logic.validateAll(players) match
          case Left(error) =>
            error.asLeft[Result].pure[F]
          case Right(_) =>
            val result = Logic.findWinners(players)
            for
              requestId <- UUIDGen[F].randomUUID
              _ <- history.save(requestId, players)
              _ <- store.save(players)
            yield result.asRight[PokerError]

      def bestHand(playerId: PlayerId): F[Option[Hand]] =
        store.find(playerId)
