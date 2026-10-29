package config

import pureconfig.ConfigReader
import scala.concurrent.duration.FiniteDuration
import pureconfig.generic.derivation.default.*

final case class ServerConfig(host: String, port: Int) derives ConfigReader

final case class PokerConfig(
    maxConcurrentRequests: Int,
    acquireTimeout: FiniteDuration
) derives ConfigReader

final case class DbConfig(url: String, user: String, password: String)
    derives ConfigReader
final case class AppConfig(
    server: ServerConfig,
    poker: PokerConfig,
    db: DbConfig
) derives ConfigReader
