package cuteguard.commands

/** Marker trait for funny commands that should not be sent in every channel
  */
trait FunnyCommand:
  this: AnyCommand =>
