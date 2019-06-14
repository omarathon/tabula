package uk.ac.warwick.tabula.data.model.permissions

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.tabula.data.model.{GeneratedId, HibernateVersioned}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}

@Entity
@Proxy
class RoleOverride extends GeneratedId with HibernateVersioned with PermissionsTarget {

  import RoleOverride._

  // optional link to some CustomRoleDefinition
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_role_definition_id")
  var customRoleDefinition: CustomRoleDefinition = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
  var permission: Permission = _

  var overrideType: OverrideType = _

  def permissionsParents: Stream[CustomRoleDefinition] = Option(customRoleDefinition).toStream

}

object RoleOverride {
  type OverrideType = Boolean
  val Allow: OverrideType = true
  val Deny: OverrideType = false
}