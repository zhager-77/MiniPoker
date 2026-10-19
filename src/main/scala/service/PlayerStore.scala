package service

import cats.syntax.all.*
import cats.effect.{Ref, Sync}
import logic.Logic
import poker.Ids.PlayerId
import poker.{Hand, Player}

trait PlayerStore[F[_]]:
  def save(players: List[Player]): F[Unit]
  def find(playerId: PlayerId): F[Option[Hand]]

//лучшие руки хранятся отдельно, так как в тз требуется О(1), запрос БД так не умеет
object PlayerStore:
  def make[F[_]: Sync]: F[PlayerStore[F]] =
    Ref.of[F, Map[PlayerId, Hand]](Map.empty).map { ref =>
      new PlayerStore[F]:
        def save(players: List[Player]): F[Unit] = {
          ref.update(current =>
            players.foldLeft(current)((acc, p) =>
              acc.updatedWith(p.playerId) {
                case None       => Some(p.hand)
                case Some(best) => Some(Logic.strongerHand(best, p.hand))
              }
            )
          )
        }
        def find(playerId: PlayerId): F[Option[Hand]] =
          ref.get.map(players => players.get(playerId))
    }
