package logic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import poker.*
import poker.Card.{Rank, Suit}
import poker.Hand.Combination
import poker.Ids.PlayerId

class LogicTest extends AnyFunSuite with Matchers:

  private def card(s: String): Card =
    val rank = s.init match
      case "2"        => Rank.Two
      case "3"        => Rank.Three
      case "4"        => Rank.Four
      case "5"        => Rank.Five
      case "6"        => Rank.Six
      case "7"        => Rank.Seven
      case "8"        => Rank.Eight
      case "9"        => Rank.Nine
      case "T" | "10" => Rank.Ten
      case "J"        => Rank.Jack
      case "Q"        => Rank.Queen
      case "K"        => Rank.King
      case "A"        => Rank.Ace
      case other => throw IllegalArgumentException(s"bad rank in test: $other")

    val suit = s.last match
      case 'h'   => Suit.Hearts
      case 'd'   => Suit.Diamonds
      case 'c'   => Suit.Clubs
      case 's'   => Suit.Spades
      case other => throw IllegalArgumentException(s"bad suit in test: $other")

    Card(suit, rank)

  private def cards(s: String): List[Card] = s.split(" ").toList.map(card)

  private def player(id: String, s: String): Player =
    val cs = cards(s)
    Player(PlayerId(id), Hand(Logic.evaluate(cs), cs))

  test("evaluate: Pair") {
    Logic.evaluate(cards("9h 9d Kc 7s 3h")) shouldBe Combination.Pair
  }

  test("evaluate: HighCard") {
    Logic.evaluate(cards("Ah Kd 9c 7s 3h")) shouldBe Combination.HighCard
  }

  test("evaluate: TwoPairs") {
    Logic.evaluate(cards("9h 9d Kc Ks 3h")) shouldBe Combination.TwoPairs
  }

  test("evaluate: ThreeOfAKind") {
    Logic.evaluate(cards("9h 9d 9c Ks 3h")) shouldBe Combination.ThreeOfAKind
  }

  test("evaluate: Straight") {
    Logic.evaluate(cards("5h 6d 7c 8s 9h")) shouldBe Combination.Straight
  }

  test("evaluate: Straight — колесо A-2-3-4-5") {
    Logic.evaluate(cards("Ah 2d 3c 4s 5h")) shouldBe Combination.Straight
  }

  test("evaluate: Flush") {
    Logic.evaluate(cards("2h 7h 9h Jh Kh")) shouldBe Combination.Flush
  }

  test("evaluate: FullHouse") {
    Logic.evaluate(cards("9h 9d 9c Ks Kh")) shouldBe Combination.FullHouse
  }

  test("evaluate: FourOfAKind") {
    Logic.evaluate(cards("9h 9d 9c 9s Kh")) shouldBe Combination.FourOfAKind
  }

  test("evaluate: StraightFlush") {
    Logic.evaluate(cards("5h 6h 7h 8h 9h")) shouldBe Combination.StraightFlush
  }

  test("evaluate: колесо одной масти, StraightFlush, а не RoyalFlush") {
    Logic.evaluate(cards("Ah 2h 3h 4h 5h")) shouldBe Combination.StraightFlush
  }

  test("evaluate: RoyalFlush") {
    Logic.evaluate(cards("Th Jh Qh Kh Ah")) shouldBe Combination.RoyalFlush
  }

  import Ordering.Implicits.*

  test("handRank: пара двоек сильнее старшей карты с тузом") {
    val pair = Logic.handRank(cards("2h 2d 5c 7s 9h"))
    val high = Logic.handRank(cards("Ah Kd Qc Js 9h"))
    assert(pair > high, s"$pair должен быть больше $high")
  }

  test("handRank: пара королей сильнее пары девяток") {
    val kings = Logic.handRank(cards("Kh Kd 2c 3s 4h"))
    val nines = Logic.handRank(cards("9h 9d Ac Qs Jh"))
    assert(kings > nines, s"$kings должен быть больше $nines")
  }

  test("handRank: при равной паре решает третий кикер") {
    val a = Logic.handRank(cards("Kh Kd 9c 7s 3h"))
    val b = Logic.handRank(cards("Ks Kc 9d 7h 5d"))
    assert(b > a, s"$b должен быть больше $a")
  }

  test("handRank: две пары сравниваются по старшей паре") {
    val kk22 = Logic.handRank(cards("Kh Kd 2c 2s 3h"))
    val qqjj = Logic.handRank(cards("Qh Qd Jc Js Ah"))
    assert(kk22 > qqjj, s"$kk22 должен быть больше $qqjj")
  }

  test("handRank: одинаковые по силе руки разных мастей равны") {
    Logic.handRank(cards("Kh Kd 9c 7s 3h")) shouldBe Logic.handRank(
      cards("Ks Kc 9d 7h 3d")
    )
  }

  test("handRank: колесо A-2-3-4-5 слабее стрита 2-3-4-5-6") {
    val wheel = Logic.handRank(cards("Ah 2d 3c 4s 5h"))
    val sixHigh = Logic.handRank(cards("2h 3d 4c 5s 6h"))
    assert(
      sixHigh > wheel,
      s"стрит до шестёрки $sixHigh должен быть сильнее колеса $wheel"
    )
  }

  test("findWinners: один победитель") {
    val p1 = player("1", "Kd Kd 9c 7s 3h")
    val p2 = player("2", "Ah Qd 9d 7h 3d")
    val result = Logic.findWinners(List(p1, p2))
    result.winners shouldBe List(p1)
    result.losers shouldBe List(p2)
  }

  test("findWinners: ничья, оба в winners, losers пуст") {
    val p1 = player("1", "Kh Kd 9c 7s 3h")
    val p2 = player("2", "Ks Kc 9d 7h 3d")
    val result = Logic.findWinners(List(p1, p2))
    result.winners.toSet shouldBe Set(p1, p2)
    result.losers shouldBe empty
  }

  test("findWinners: три игрока, побеждает сильнейшая комбинация") {
    val flush = player("1", "2h 7h 9h Jh Kh")
    val pair = player("2", "As Ad 9c 7s 3c")
    val high = player("3", "Kc Qs Jd 8h 4d")
    val result = Logic.findWinners(List(flush, pair, high))
    result.winners shouldBe List(flush)
    result.losers.toSet shouldBe Set(pair, high)
  }

  test("ValidateNoDuplicates: дубль между разными игроками") {
    val p1 = player("1", "Kh Kd 9c 7s 3h")
    val p2 = player("2", "Kh Qd 8d 6h 2d")
    Logic.validateNoDuplicates(List(p1, p2)) shouldBe Left(
      PokerError.DuplicateCard(card("Kh"))
    )
  }

  test("validateNoDuplicates: без дублей, Right") {
    val p1 = player("1", "Kh Kd 9c 7s 3h")
    val p2 = player("2", "Ah Qd 8d 6h 2d")
    Logic.validateNoDuplicates(List(p1, p2)) shouldBe Right(())
  }

  test("validateHandSize: 5 карт, Right") {
    Logic.validateHandSize(player("1", "Kh Kd 9c 7s 3h")) shouldBe Right(())
  }

  test("validateHandSize: 4 карты, ошибка с id игрока и размером") {
    val p = Player(PlayerId("7"), Hand(Combination.Pair, cards("Kh Kd 9c 7s")))
    Logic.validateHandSize(p) shouldBe Left(
      PokerError.InvalidHandSize(PlayerId("7"), 4)
    )
  }

  test("validateCombination: заявленная комбинация совпала, Right") {
    Logic.validateCombination(player("1", "9h 9d Kc 7s 3h")) shouldBe Right(())
  }

  test("validateCombination: клиент заявил RoyalFlush на мусоре") {
    val p = Player(
      PlayerId("1"),
      Hand(Combination.RoyalFlush, cards("2d 7c 9h Js 3d"))
    )
    Logic.validateCombination(p) shouldBe
      Left(
        PokerError.InvalidCombination(
          Combination.RoyalFlush,
          Combination.HighCard
        )
      )
  }

  test("validateAll: всё корректно, Right") {
    val p1 = player("1", "Kh Kd 9c 7s 3h")
    val p2 = player("2", "Ah Qd 8d 6h 2d")
    Logic.validateAll(List(p1, p2)) shouldBe Right(())
  }

  test("validateAll: короткое замыкание, дубль обнаружен раньше размера руки") {
    val p1 = player("1", "Kh Kd 9c 7s 3h")
    val p2 = Player(PlayerId("2"), Hand(Combination.HighCard, cards("Kh 2d")))
    Logic.validateAll(List(p1, p2)) match
      case Left(_: PokerError.DuplicateCard) => succeed
      case other => fail(s"ожидался DuplicateCard, получено: $other")
  }
