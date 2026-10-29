package poker

final case class Stats(
    totalRequests: Int,
    inFlight: Int,
    rejectedRequests: Int,
    totalProcessingMs: Long
):

  def avgProcessingMs: Long =
    if totalRequests == 0 then 0
    else totalProcessingMs / totalRequests