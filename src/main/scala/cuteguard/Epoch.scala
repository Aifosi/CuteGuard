package cuteguard

import cats.Show

import java.time.Instant

opaque type Epoch         = Long
opaque type ShortTime     = Long
opaque type LongTime      = Long
opaque type ShortDate     = Long
opaque type LongDate      = Long
opaque type ShortTimeDate = Long
opaque type LongTimeDate  = Long
opaque type RelativeTime  = Long

object Epoch:
  given Show[Epoch] = Show.show(long => s"<t:$long>")

  extension (instant: Instant) def toEpoch: Long = Epoch(instant)

  def apply(instant: Instant): Epoch = instant.getEpochSecond

extension (epoch: Epoch)
  def toShortTime: ShortTime         = ShortTime(epoch)
  def toLongTime: LongTime           = LongTime(epoch)
  def toShortDate: ShortDate         = ShortDate(epoch)
  def toLongDate: LongDate           = LongDate(epoch)
  def toShortTimeDate: ShortTimeDate = ShortTimeDate(epoch)
  def toLongTimeDate: LongTimeDate   = LongTimeDate(epoch)
  def toRelativeTime: RelativeTime   = RelativeTime(epoch)

object ShortTime:
  given Show[ShortTime]              = Show.show(long => s"<t:$long:t>")
  def apply(epoch: Epoch): ShortTime = epoch.toLong

object LongTime:
  given Show[LongTime]              = Show.show(long => s"<t:$long:T>")
  def apply(epoch: Epoch): LongTime = epoch.toLong

object ShortDate:
  given Show[ShortDate]              = Show.show(long => s"<t:$long:d>")
  def apply(epoch: Epoch): ShortDate = epoch.toLong

object LongDate:
  given Show[LongDate]              = Show.show(long => s"<t:$long:D>")
  def apply(epoch: Epoch): LongDate = epoch.toLong

object ShortTimeDate:
  given Show[ShortTimeDate]              = Show.show(long => s"<t:$long:f>")
  def apply(epoch: Epoch): ShortTimeDate = epoch.toLong

object LongTimeDate:
  given Show[LongTimeDate]              = Show.show(long => s"<t:$long:F>")
  def apply(epoch: Epoch): LongTimeDate = epoch.toLong

object RelativeTime:
  given Show[RelativeTime]              = Show.show(long => s"<t:$long:R>")
  def apply(epoch: Epoch): RelativeTime = epoch.toLong
