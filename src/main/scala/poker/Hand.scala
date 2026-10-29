package poker

object Hand:

  enum Combination(val weight: Int):
    case HighCard extends Combination(1)
    case Pair extends Combination(2)
    case TwoPairs extends Combination(3)
    case ThreeOfAKind extends Combination(4)
    case Straight extends Combination(5)
    case Flush extends Combination(6)
    case FullHouse extends Combination(7)
    case FourOfAKind extends Combination(8)
    case StraightFlush extends Combination(9)
    case RoyalFlush extends Combination(10)

import poker.Hand.Combination

final case class Hand(combination: Combination, cards: List[Card])
