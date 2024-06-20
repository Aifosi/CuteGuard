package cuteguard.model

import net.dv8tion.jda.api.EmbedBuilder

object Embed:
  def apply(title: String, description: String, image: String, footer: String) = new EmbedBuilder()
    .setTitle(title)
    .setDescription(description)
    .setImage(image)
    .setFooter(footer)
    .build()

  def apply(title: String, image: String, footer: String) = new EmbedBuilder()
    .setTitle(title)
    .setImage(image)
    .setFooter(footer)
    .build()
