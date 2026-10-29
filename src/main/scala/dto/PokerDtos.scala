package dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object PokerDtos:

  final case class UserHandDto(id: String, hand: HandDto)
  object UserHandDto:
    given Decoder[UserHandDto] = deriveDecoder[UserHandDto]
    given Encoder[UserHandDto] = deriveEncoder[UserHandDto]

  final case class HandDto(combination: String, cards: List[CardDto])
  object HandDto:
    given Decoder[HandDto] = deriveDecoder[HandDto]
    given Encoder[HandDto] = deriveEncoder[HandDto]

  final case class StatsDto(totalRequests: Int, inFlight: Int, rejectedRequests: Int, avgProcessingMs: Long)
  object StatsDto:
    given Encoder[StatsDto] = deriveEncoder[StatsDto]

  final case class CardDto(suit: String, rank: String)
  object CardDto:
    given Decoder[CardDto] = deriveDecoder[CardDto]
    given Encoder[CardDto] = deriveEncoder[CardDto]

  final case class EvaluatePlayersDto(
      winners: List[UserHandDto],
      losers: List[UserHandDto]
  )
  object EvaluatePlayersDto:
    given Encoder[EvaluatePlayersDto] = deriveEncoder[EvaluatePlayersDto]

  final case class StatusMessage(message: String)
  object StatusMessage:
    given Encoder[StatusMessage] = deriveEncoder[StatusMessage]
