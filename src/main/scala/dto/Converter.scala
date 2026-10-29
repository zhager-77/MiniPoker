package dto

import poker.*
import dto.PokerDtos.*
import poker.Card.{Rank, Suit}
import poker.Hand.Combination
import poker.Ids.PlayerId
import cats.syntax.all.*
import logic.Logic.*

object Converter:

  def parseSuit(raw: String): Either[PokerError, Suit] =
    raw match {
      case "Spades" | "s" | "♠"   => Right(Suit.Spades)
      case "Clubs" | "c" | "♣"    => Right(Suit.Clubs)
      case "Hearts" | "h" | "♥"   => Right(Suit.Hearts)
      case "Diamonds" | "d" | "♦" => Right(Suit.Diamonds)
      case other                  => Left(PokerError.InvalidSuit(other))
    }

  def parseRank(raw: String): Either[PokerError, Rank] =
    raw match {
      case "Ace" | "A"   => Right(Rank.Ace)
      case "King" | "K"  => Right(Rank.King)
      case "Queen" | "Q" => Right(Rank.Queen)
      case "Jack" | "J"  => Right(Rank.Jack)
      case "Ten" | "T"   => Right(Rank.Ten)
      case "Nine" | "9"  => Right(Rank.Nine)
      case "Eight" | "8" => Right(Rank.Eight)
      case "Seven" | "7" => Right(Rank.Seven)
      case "Six" | "6"   => Right(Rank.Six)
      case "Five" | "5"  => Right(Rank.Five)
      case "Four" | "4"  => Right(Rank.Four)
      case "Three" | "3" => Right(Rank.Three)
      case "Two" | "2"   => Right(Rank.Two)
      case other         => Left(PokerError.InvalidRank(other))
    }

  def parseCombination(raw: String): Either[PokerError, Combination] =
    raw match {
      case "HighCard" | "HC"      => Right(Combination.HighCard)
      case "Pair" | "1P"          => Right(Combination.Pair)
      case "TwoPairs" | "2P"      => Right(Combination.TwoPairs)
      case "ThreeOfAKind" | "3K"  => Right(Combination.ThreeOfAKind)
      case "Straight" | "S"       => Right(Combination.Straight)
      case "Flush" | "F"          => Right(Combination.Flush)
      case "FullHouse" | "FH"     => Right(Combination.FullHouse)
      case "FourOfAKind" | "4K"   => Right(Combination.FourOfAKind)
      case "StraightFlush" | "SF" => Right(Combination.StraightFlush)
      case "RoyalFlush" | "RF"    => Right(Combination.RoyalFlush)
      case other                  => Left(PokerError.UnknownCombination(other))
    }

  def toCard(dto: CardDto): Either[PokerError, Card] =
    for
      suit <- parseSuit(dto.suit)
      rank <- parseRank(dto.rank)
    yield Card(suit, rank)

  def toHand(dto: HandDto): Either[PokerError, Hand] =
    for
      combination <- parseCombination(dto.combination)
      cards <- dto.cards.traverse(toCard)
    yield Hand(combination, cards)

  def toPlayer(dto: UserHandDto): Either[PokerError, Player] =
    for hand <- toHand(dto.hand)
    yield Player(PlayerId(dto.id), hand)

  // Выбрал самую читаемую форму на выход
  def fromCard(card: Card): CardDto =
    CardDto(card.suit.toString, card.rank.toString)

  def fromHand(hand: Hand): HandDto =
    HandDto(
      hand.combination.toString,
      sortByRelevance(hand.cards).map(fromCard)
    )

  def fromPlayer(player: Player): UserHandDto =
    UserHandDto(player.playerId.value, fromHand(player.hand))

  def readableCard(card: Card): String =
    s"${card.rank} of ${card.suit}"
