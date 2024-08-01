package cuteguard.syntax

import java.time.{Instant, LocalDate, ZoneOffset}

trait LocalDateSyntax:
  extension (date: LocalDate) def atStartOfDayUTC: Instant = date.atStartOfDay.toInstant(ZoneOffset.UTC)
