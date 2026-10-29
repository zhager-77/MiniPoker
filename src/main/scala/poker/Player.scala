package poker

object Ids:
  opaque type PlayerId = String
  object PlayerId:
    def apply(value: String): PlayerId = value
    extension (id: PlayerId) def value: String = id

import poker.Ids.PlayerId

final case class Player(playerId: PlayerId, hand: Hand)
