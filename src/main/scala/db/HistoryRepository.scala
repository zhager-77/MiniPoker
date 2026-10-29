package db

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.h2.implicits.*
import java.util.UUID
import poker.{Card, Player}
import poker.Card.{Rank, Suit}

// одна строка = рука одного игрока, карты хранятся в своем формате
trait HistoryRepository[F[_]]:
  def save(requestID: UUID, players: List[Player]): F[Unit]

object HistoryRepository:

  private val createTable: Update0 =
    sql"""
         CREATE TABLE IF NOT EXISTS hand_history (
         id BIGINT AUTO_INCREMENT PRIMARY KEY,
         request_id UUID NOT NULL,
         player_id VARCHAR(64) NOT NULL,
         cards VARCHAR(20) NOT NULL,
         combination VARCHAR(20) NOT NULL,
         weight INT NOT NULL
         )
         """.update

  private type Row = (UUID, String, String, String, Int)

  private val insert: Update[Row] =
    Update[Row](
      "INSERT INTO hand_history (request_id, player_id, cards, combination, weight) VALUES (?, ?, ?, ?, ?)"
    )

  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): F[HistoryRepository[F]] =
    createTable.run.transact(xa).as {
      new HistoryRepository[F]:
        def save(requestID: UUID, players: List[Player]): F[Unit] =
          val rows = players.map(p =>
            (
              requestID,
              p.playerId.value,
              encodeCards(p.hand.cards),
              p.hand.combination.toString,
              p.hand.combination.weight
            )
          )
          insert.updateMany(rows).transact(xa).void
    }

  private def encodeCards(cards: List[Card]): String =
    cards.map(encodeCard).mkString(" ")

  private def encodeCard(card: Card): String =
    val rank = card.rank match
      case Rank.Two   => "2"
      case Rank.Three => "3"
      case Rank.Four  => "4"
      case Rank.Five  => "5"
      case Rank.Six   => "6"
      case Rank.Seven => "7"
      case Rank.Eight => "8"
      case Rank.Nine  => "9"
      case Rank.Ten   => "T"
      case Rank.Jack  => "J"
      case Rank.Queen => "Q"
      case Rank.King  => "K"
      case Rank.Ace   => "A"

    val suit = card.suit match
      case Suit.Diamonds => "d"
      case Suit.Hearts   => "h"
      case Suit.Clubs    => "c"
      case Suit.Spades   => "s"

    rank + suit
