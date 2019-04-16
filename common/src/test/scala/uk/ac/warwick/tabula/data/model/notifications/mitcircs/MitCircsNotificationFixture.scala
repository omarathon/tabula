package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.mockito.Mockito.when
import uk.ac.warwick.tabula.data.model.{Department, UserGroup}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesOfficerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.{Fixtures, Mockito}
import uk.ac.warwick.userlookup.User


trait MitCircsNotificationFixture {

  self: Mockito =>

  val submission: MitigatingCircumstancesSubmission = Fixtures.mitigatingCircumstancesSubmission("student", "student")
  submission.key = 1000l
  val student: User = submission.student.asSsoUser
  val admin: User = Fixtures.user("admin", "admin")
  val mcoRole: GrantedRole[Department] = new GrantedRole[Department]
  mcoRole.users.add(admin)
  mcoRole.users.asInstanceOf[UserGroup].userLookup = Fixtures.userLookupService(admin)
  lazy val mockPermissionsService: PermissionsService = smartMock[PermissionsService]
  when(mockPermissionsService.getGrantedRole(submission.department, MitigatingCircumstancesOfficerRoleDefinition)).thenReturn(Some(mcoRole))

}
