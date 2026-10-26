package db

import cats.MonadThrow
import cats.effect.{MonadCancelThrow, Ref}
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.h2.implicits.*
import logic.Logic

import java.util.UUID
import poker.{Card, Hand, Player, PokerError}
import poker.Card.{Rank, Suit}
import poker.Ids.PlayerId

// одна строка = рука одного игрока, карты хранятся в своем формате
trait HistoryRepository[F[_]]:
  def save(requestID: UUID, players: List[Player]): F[Unit]
  def loadAll: F[List[Player]]

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

        def loadAll: F[List[Player]] = {
          sql"SELECT player_id, cards FROM hand_history"
            .query[(String, String)]
            .to[List]
            .transact(xa)
            .flatMap(rows =>
              rows.traverse(rowToPlayer) match {
                case Right(players) => players.pure[F]
                case Left(error) =>
                  MonadThrow[F].raiseError(
                    new RuntimeException(s"Битая запись в историй: $error")
                  )
              }
            )
        }
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

  private def decodeCard(card: String): Either[PokerError, Card] =

    if card.length != 2 then Left(PokerError.InvalidRank("Invalid format"))
    else
      val rank: Either[PokerError, Rank] = card.init match
        case "2"   => Right(Rank.Two)
        case "3"   => Right(Rank.Three)
        case "4"   => Right(Rank.Four)
        case "5"   => Right(Rank.Five)
        case "6"   => Right(Rank.Six)
        case "7"   => Right(Rank.Seven)
        case "8"   => Right(Rank.Eight)
        case "9"   => Right(Rank.Nine)
        case "T"   => Right(Rank.Ten)
        case "J"   => Right(Rank.Jack)
        case "Q"   => Right(Rank.Queen)
        case "K"   => Right(Rank.King)
        case "A"   => Right(Rank.Ace)
        case other => Left(PokerError.InvalidRank(other))

      val suit: Either[PokerError, Suit] = card.last match
        case 'd'   => Right(Suit.Diamonds)
        case 'h'   => Right(Suit.Hearts)
        case 'c'   => Right(Suit.Clubs)
        case 's'   => Right(Suit.Spades)
        case other => Left(PokerError.InvalidSuit(other.toString))

      for
        r <- rank
        s <- suit
      yield Card(s, r)

  private def decodeCards(raw: String): Either[PokerError, List[Card]] =
    raw.split(" ").toList.traverse(decodeCard)

  private def rowToPlayer(id: String, raw: String): Either[PokerError, Player] =
    decodeCards(raw) match {
      case Right(cards) =>
        Right(Player(PlayerId(id), Hand(Logic.evaluate(cards), cards)))
      case Left(error) => Left(error)
    }
