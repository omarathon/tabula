package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.DateTime
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition
import uk.ac.warwick.tabula.services.ExtensionService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

trait ExtensionFixture extends Mockito {

  val studentMember = new StudentMember
  studentMember.universityId = "1234657"
  studentMember.userId = "u1234567"
  studentMember.firstName = "John"
  studentMember.lastName = "Smith"
  studentMember.email = "J.Smith@tabula.warwick.ac.uk"

  val studentCourseDetails = new StudentCourseDetails(studentMember, "1234567/1")
  studentMember.mostSignificantCourse = studentCourseDetails
  studentCourseDetails.currentRoute = Fixtures.route("t100", "Tabula Studies (BEng)")
  studentCourseDetails.currentRoute.degreeType = DegreeType.Undergraduate
  studentCourseDetails.course = Fixtures.course("UTAB-T100", "Tabula Studies")
  studentCourseDetails.award = new Award("BENG", "BEng")

  val student: User = studentMember.asSsoUser

  val adminMember = new StaffMember
  adminMember.universityId = "admin"
  adminMember.userId = "admin"
  val admin: User = adminMember.asSsoUser

  val adminMember2 = new StaffMember
  adminMember2.universityId = "admin2"
  adminMember2.userId = "admin2"
  val admin2: User = adminMember2.asSsoUser

  val adminMember3 = new StaffMember
  adminMember3.universityId = "admin3"
  adminMember3.userId = "admin3"
  val admin3: User = adminMember3.asSsoUser

  val otherAdmins = Seq(admin2, admin3)

  val userLookup = new MockUserLookup
  userLookup.users = Map("admin" -> admin, "admin2" -> admin2, "admin3" -> admin3)
  val extensionManagers: UserGroup = UserGroup.ofUsercodes
  extensionManagers.userLookup = userLookup
  extensionManagers.includedUserIds = Set("admin", "admin2", "admin3")

  val department = new Department
  val permissionsService: PermissionsService = mock[PermissionsService]
  when(permissionsService.ensureUserGroupFor(department, ExtensionManagerRoleDefinition)) thenReturn extensionManagers
  department.permissionsService = permissionsService

  val module = new Module {
    override lazy val managers = UserGroup.ofUsercodes
    managers.userLookup = userLookup
    userLookup.registerUsers("manager1", "manager2")
    managers.includedUserIds = Set("manager1", "manager2")
  }
  module.adminDepartment = department
  module.code = "xxx"
  module.name = "Module"

  val assignment = new Assignment
  val extensionService: ExtensionService = smartMock[ExtensionService]
  assignment.extensionService = extensionService

  extensionService.hasExtensions(any[Assignment]) answers { assignmentObj =>
    val assignment = assignmentObj.asInstanceOf[Assignment]
    !assignment._extensions.isEmpty
  }
  extensionService.getApprovedExtensionsByUserId(any[Assignment]) answers { assignmentObj =>
    val assignment = assignmentObj.asInstanceOf[Assignment]
    assignment._extensions.asScala.filter(_.approved)
      .groupBy(_.usercode)
      .mapValues(_.maxBy(_.expiryDate.map(_.getMillis).getOrElse(0L)))
  }

  assignment.name = "Essay"
  assignment.id = "123"
  assignment.openEnded = false
  assignment.closeDate = new DateTime(2013, 8, 1, 12, 0)
  assignment.module = module

  val extension = new Extension
  extension.id = "someID"
  extension._universityId = student.getWarwickId
  extension.usercode = student.getUserId
  extension.expiryDate = new DateTime(2013, 8, 23, 12, 0)
  extension.requestedExpiryDate = new DateTime(2013, 8, 23, 12, 0)
  extension.reason = "My hands have turned to flippers. Like the ones that dolphins have.\n\nIt makes writing and typing super hard. Pity me."
  extension.reviewerComments = "That sounds awful. Have an extra month.\n\nBy then you should be able to write as well as any Cetacea."
  extension.assignment = assignment
  extension.approve()
  assignment.addExtension(extension)
}
