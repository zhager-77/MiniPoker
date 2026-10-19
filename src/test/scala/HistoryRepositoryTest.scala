package db

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.util.UUID
import poker.{Card, Hand, Player}
import poker.Card.{Rank, Suit}
import poker.Hand.Combination
import poker.Ids.PlayerId

class HistoryRepositorySuite extends AnyFunSuite with Matchers:

  
  private def freshXa(): Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = "org.h2.Driver",
      url = s"jdbc:h2:mem:test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
      user = "sa",
      password = "",
      logHandler = None
    )

  private val p1 = Player(
    PlayerId("p1"),
    Hand(
      Combination.Pair,
      List(
        Card(Suit.Hearts, Rank.King),
        Card(Suit.Diamonds, Rank.King),
        Card(Suit.Clubs, Rank.Nine),
        Card(Suit.Spades, Rank.Seven),
        Card(Suit.Hearts, Rank.Ten)
      )
    )
  )

  private val p2 = Player(
    PlayerId("p2"),
    Hand(
      Combination.HighCard,
      List(
        Card(Suit.Hearts, Rank.Ace),
        Card(Suit.Diamonds, Rank.Queen),
        Card(Suit.Diamonds, Rank.Eight),
        Card(Suit.Clubs, Rank.Six),
        Card(Suit.Spades, Rank.Two)
      )
    )
  )

  test(
    "история накапливается: два запроса по два игрока — 4 строки, 2 разных request_id"
  ) {
    val xa = freshXa()

    val (total, requests) = (for
      repo <- HistoryRepository.make[IO](xa)
      _ <- repo.save(UUID.randomUUID(), List(p1, p2))
      _ <- repo.save(UUID.randomUUID(), List(p1, p2))
      total <- sql"SELECT COUNT(*) FROM hand_history"
        .query[Long]
        .unique
        .transact(xa)
      requests <- sql"SELECT COUNT(DISTINCT request_id) FROM hand_history"
        .query[Long]
        .unique
        .transact(xa)
    yield (total, requests)).unsafeRunSync()

    total shouldBe 4
    requests shouldBe 2
  }

  test("рука сохраняется в формате хранения: карты, комбинация, вес") {
    val xa = freshXa()

    val rows = (for
      repo <- HistoryRepository.make[IO](xa)
      _ <- repo.save(UUID.randomUUID(), List(p1))
      rows <-
        sql"SELECT cards, combination, weight FROM hand_history WHERE player_id = 'p1'"
          .query[(String, String, Int)]
          .to[List]
          .transact(xa)
    yield rows).unsafeRunSync()

    rows shouldBe List(("Kh Kd 9c 7s Th", "Pair", 2))
  }
