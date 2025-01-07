package cuteguard.syntax

import cuteguard.*

import java.time.{Instant, LocalDate, ZoneOffset}

trait LocalDateSyntax:
  extension (date: LocalDate)
    def atStartOfDayUTC: Instant = date.atStartOfDay.toInstant(ZoneOffset.UTC)

    def toEpoch: Epoch                 = Epoch(date.atStartOfDayUTC)
    def toShortTime: ShortTime         = ShortTime(date.toEpoch)
    def toLongTime: LongTime           = LongTime(date.toEpoch)
    def toShortDate: ShortDate         = ShortDate(date.toEpoch)
    def toLongDate: LongDate           = LongDate(date.toEpoch)
    def toShortTimeDate: ShortTimeDate = ShortTimeDate(date.toEpoch)
    def toLongTimeDate: LongTimeDate   = LongTimeDate(date.toEpoch)
    def toRelativeTime: RelativeTime   = RelativeTime(date.toEpoch)
