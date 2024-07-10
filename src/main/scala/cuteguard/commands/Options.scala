package cuteguard.commands

type PatternOption = SlashPattern => SlashPattern

trait Options:
  this: SlashCommand =>
  val options: List[PatternOption]

  override def pattern: SlashPattern = slashPattern.applyOptions(options)
