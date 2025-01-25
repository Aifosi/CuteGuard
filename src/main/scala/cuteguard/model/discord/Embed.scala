package cuteguard.model.discord

import net.dv8tion.jda.api.EmbedBuilder

import java.awt.Color

object Embed:
  def apply(title: String, description: String, image: String) = new EmbedBuilder()
    .setTitle(title)
    .setDescription(description)
    .setImage(image)
    .setColor(new Color(75, 0, 150))
    .build()

  def apply(title: String, image: String) = new EmbedBuilder()
    .setTitle(title)
    .setImage(image)
    .setColor(new Color(75, 0, 150))
    .build()
