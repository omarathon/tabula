package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.NotificationWithTarget
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesOfficerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.userlookup.User

trait NotifiesMitCircsOfficers {

  self: NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission] =>

  @transient
  var permissionsService: PermissionsService = Wire.auto[PermissionsService]

  override def recipients: Seq[User] = permissionsService.getGrantedRole(target.entity.department, MitigatingCircumstancesOfficerRoleDefinition)
    .map(_.users.users.toSeq).getOrElse(Seq())

}
