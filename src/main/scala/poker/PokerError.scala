package poker
import poker.Hand.Combination
import poker.Ids.PlayerId

enum PokerError:
  case DuplicateCard(card: Card)
  case PlayerNotFound(playerId: PlayerId)
  case InvalidHandSize(playerId: PlayerId, size: Int)
  case InvalidCombination(
      clientCombination: Combination,
      serverCombination: Combination
  )
  case InvalidSuit(message: String)
  case InvalidRank(message: String)
  case UnknownCombination(message: String)
  case EmptyPlayerList
