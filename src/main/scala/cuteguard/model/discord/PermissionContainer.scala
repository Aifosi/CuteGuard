package cuteguard.model.discord

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer

import scala.jdk.CollectionConverters.*

class PermissionContainer(container: IPermissionContainer):
  private def permissionOverride(role: Role) = Option(container.getPermissionOverride(role.role))

  def hasAllowedOverride(role: Role, permission: Permission): Boolean =
    permissionOverride(role).exists(_.getAllowed.asScala.contains(permission))
