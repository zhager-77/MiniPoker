package api

import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import dto.PokerDtos.StatusMessage
import poker.PokerError
import dto.Converter.*
import poker.PokerError.EmptyPlayerList

object ErrorHandling:
  // Использует полные имена для удобства, и чтобы читалось в одном стиле
  def toResponse(error: PokerError): (Status, String) =
    error match {
      case PokerError.PlayerNotFound(playerId) =>
        (Status.NotFound, s"${playerId.value} not found")
      case PokerError.UnknownCombination(message) =>
        (Status.BadRequest, s"Unknown combination: $message")
      case PokerError.InvalidCombination(
            clientCombination,
            serverCombination
          ) =>
        (
          Status.BadRequest,
          s"$clientCombination invalid, required: $serverCombination"
        )
      case PokerError.InvalidRank(message) =>
        (Status.BadRequest, s"Invalid rank: $message")
      case PokerError.InvalidSuit(message) =>
        (Status.BadRequest, s"Invalid suit: $message")
      case PokerError.DuplicateCard(card) =>
        (Status.BadRequest, s"Card duplicate: ${readableCard(card)}")
      case PokerError.InvalidHandSize(playerId, size) =>
        (Status.BadRequest, s"$playerId has invalid size $size, required 5")
      case EmptyPlayerList =>
        (Status.BadRequest, "Players list must not be empty")
    }

  def toHttpResponse[F[_]](error: PokerError): Response[F] =
    val (status, message) = toResponse(error)
    Response[F](status).withEntity(StatusMessage(message))
