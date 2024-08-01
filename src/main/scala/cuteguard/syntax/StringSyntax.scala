package cuteguard.syntax

trait StringSyntax:
  extension (string: String)
    def startsWithIgnoreCase(other: String): Boolean = string.toLowerCase.startsWith(other.toLowerCase)
