package logic
import poker.{Card, Player, PokerError, Result, Hand}
import poker.Hand.Combination
import poker.PokerError.{EmptyPlayerList, InvalidCombination, InvalidHandSize}
import cats.syntax.all.*
import poker.Card.Rank.Ace

import Ordering.Implicits.*

object Logic:

  private val HandSize = 5
  // туз весит 14, поэтому колесо проверяется отдельно
  def evaluate(cards: List[Card]): Combination =
    val rankCards =
      cards.groupBy(_.rank).values.map(_.size).toList.sorted.reverse
    val isFlush = cards.map(_.suit).distinct.size == 1
    val weights = cards.map(_.rank.weight).sorted
    val isStraight = weights.max - weights.min == 4 || isWheel(cards)
    val isRoyal = weights.min == 10
    rankCards match {
      case 4 :: _      => Combination.FourOfAKind
      case 3 :: 2 :: _ => Combination.FullHouse
      case 3 :: _      => Combination.ThreeOfAKind
      case 2 :: 2 :: _ => Combination.TwoPairs
      case 2 :: _      => Combination.Pair
      case _ =>
        (isFlush, isStraight) match {
          case (true, true) =>
            if isRoyal then Combination.RoyalFlush
            else Combination.StraightFlush
          case (true, false)  => Combination.Flush
          case (false, true)  => Combination.Straight
          case (false, false) => Combination.HighCard
        }
    }
  // List(вес комбинации, ранги по важности). Сравнивается как список, учитывает комбинацию и кикера
  def handRank(cards: List[Card]): List[Int] =
    val rankWeights =
      if isWheel(cards) then List(5, 4, 3, 2, 1)
      else
        cards
          .groupBy(_.rank)
          .toList
          .sortBy(x => (-x._2.size, -x._1.weight))
          .map(x => x._1.weight)

    evaluate(cards).weight :: rankWeights

  def sortByRelevance(cards: List[Card]): List[Card] =
    if isWheel(cards) then
      cards.sortBy(card => if card.rank == Ace then -1 else -card.rank.weight)
    else
      cards
        .groupBy(_.rank)
        .toList
        .sortBy(x => (-x._2.size, -x._1.weight))
        .flatMap(x => x._2)

  // на пустом списке .max упадет, ее убирает validateNotEmpty
  def findWinners(players: List[Player]): Result =
    val ranked = players.map(player => (player, handRank(player.hand.cards)))
    val maxRank = ranked.map(_._2).max
    val (winners, losers) = ranked.partition(x => x._2 == maxRank)
    Result(winners.map(_._1), losers.map(_._1))

  def validateNoDuplicates(players: List[Player]): Either[PokerError, Unit] =
    val allCards = players.flatMap(_.hand.cards)
    allCards.groupBy(identity).find(_._2.size > 1) match {
      case Some(card) => Left(PokerError.DuplicateCard(card._1))
      case None       => Right(())
    }

  def validateHandSize(player: Player): Either[PokerError, Unit] =
    val size = player.hand.cards.size
    if size == HandSize then Right(())
    else Left(PokerError.InvalidHandSize(player.playerId, size))

  // Спека не дает информацию, что делать с комбинациями. Сделал сам, чтобы выявлять ошибки раньше
  def validateCombination(player: Player): Either[PokerError, Unit] =
    val playerCombination = player.hand.combination
    val serverCombination = evaluate(player.hand.cards)
    if playerCombination == serverCombination then Right(())
    else Left(InvalidCombination(playerCombination, serverCombination))

  def validateNotEmpty(players: List[Player]): Either[PokerError, Unit] =
    if players.isEmpty then Left(PokerError.EmptyPlayerList)
    else Right(())

  // завершаем на первой ошибке
  def validateAll(players: List[Player]): Either[PokerError, Unit] =
    for
      _ <- validateNotEmpty(players)
      _ <- validateNoDuplicates(players)
      _ <- players.traverse_(validateHandSize)
      _ <- players.traverse_(validateCombination)
    yield ()

  private def isWheel(cards: List[Card]): Boolean =
    cards.map(_.rank.weight).sorted == List(2, 3, 4, 5, 14)

  // При равном весе оставляем старую руку. Ordering.gt вместо ">", так как конфликтуют cats и ordering
  def strongerHand(previous: Hand, current: Hand): Hand =
    if Ordering[List[Int]].gt(handRank(current.cards), handRank(previous.cards))
    then current
    else previous
