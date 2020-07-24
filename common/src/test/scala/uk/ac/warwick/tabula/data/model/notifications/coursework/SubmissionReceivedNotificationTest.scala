package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SingleMarkingCompleted
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

class SubmissionReceivedNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  val userLookup = new MockUserLookup

  @Test def titleOnTime(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    withUser("cuscav", "0672089") {
      val assignment = Fixtures.assignment("5,000 word essay")
      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      assignment.isLate(submission) should be(false)
      assignment.isAuthorisedLate(submission) should be(false)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Submission received for \"5,000 word essay\"")
    }
  }


  @Test def titleOnTimeAEPSubmission(): Unit = withFakeTime(new DateTime(2019, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    withUser("cuscao", "0270954") {
      val assignment = Fixtures.assignment("AEP exam")
      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      // closeDate is before submission, but we have explicit Submission deadline
      assignment.closeDate = new DateTime(2019, DateTimeConstants.SEPTEMBER, 14, 9, 0, 0, 0)
      assignment.createdByAEP = true

      val submission = Fixtures.submission()
      submission.assignment = assignment

      submission.submittedDate = DateTime.now
      // explicit submission deadline is after submission date
      submission.explicitSubmissionDeadline = new DateTime(2019, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      assignment.isLate(submission) should be(false)
      assignment.isAuthorisedLate(submission) should be(false)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Submission received for \"AEP exam\"")
    }
  }

  @Test def titleOnTimeBeforeExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    withUser("cuscav", "0672089") {
      val assignment = Fixtures.assignment("5,000 word essay")
      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      val extension = Fixtures.extension()
      extension.assignment = assignment
      extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
      extension.approve()
      assignment.addExtension(extension)

      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(extension.usercode -> extension)

      assignment.isLate(submission) should be(false)
      assignment.isAuthorisedLate(submission) should be(false)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Submission received for \"5,000 word essay\"")
    }
  }

  @Test def titleLate(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) {
    withUser("cuscav", "0672089") {
      val assignment = Fixtures.assignment("5,000 word essay")
      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      assignment.isLate(submission) should be(true)
      assignment.isAuthorisedLate(submission) should be(false)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Late submission received for \"5,000 word essay\"")
    }
  }

  @Test def titleLateAEPSubmission(): Unit = withFakeTime(new DateTime(2019, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) {
    withUser("cuscao", "0270954") {
      val assignment = Fixtures.assignment("AEP exam")
      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      // assignment close date is irrelevant for AEP submissions
      assignment.closeDate = new DateTime(2019, DateTimeConstants.SEPTEMBER, 16, 20, 0, 0, 0)
      assignment.createdByAEP = true

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now
      submission.explicitSubmissionDeadline = new DateTime(2019, DateTimeConstants.SEPTEMBER, 11, 20, 0, 0, 0)

      assignment.isLate(submission) should be(true)
      assignment.isAuthorisedLate(submission) should be(false)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Late submission received for \"AEP exam\"")
    }
  }

  @Test def titleLateWithinExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) {
    withUser("cuscav", "0672089") {
      val assignment = Fixtures.assignment("5,000 word essay")
      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      val extension = Fixtures.extension()
      extension.assignment = assignment
      extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
      extension.approve()
      assignment.addExtension(extension)

      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(extension.usercode -> extension)

      assignment.isLate(submission) should be(false)
      assignment.isAuthorisedLate(submission) should be(true)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Authorised late submission received for \"5,000 word essay\"")
    }
  }

  @Test def titleLateAfterExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    withUser("cuscav", "0672089") {
      val assignment = Fixtures.assignment("5,000 word essay")
      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

      assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      val extension = Fixtures.extension()
      extension.assignment = assignment
      extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
      extension.approve()
      assignment.addExtension(extension)

      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(extension.usercode -> extension)

      assignment.isLate(submission) should be(true)
      assignment.isAuthorisedLate(submission) should be(false)

      val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      notification.title should be("CS118: Late submission received for \"5,000 word essay\"")
    }
  }

  @Test def recipientsForLateNotificationWithNoAdminForSubDept(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    withUser("cuscav", "0672089") {
      val securityService = mock[SecurityService]
      val permissionsService = mock[PermissionsService]
      val service = mock[UserSettingsService]

      //create dept with one sub dept
      val department = Fixtures.department("ch")
      val subDepartment = Fixtures.department("ch-ug")
      subDepartment.parent = department

      val assignment = Fixtures.assignment("5,000 word essay")
      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

      val module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.module = module
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

      assignment.feedbackService = smartMock[FeedbackService]
      assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

      val submission = Fixtures.submission()
      submission.assignment = assignment
      submission.submittedDate = DateTime.now
      module.adminDepartment = subDepartment

      val adminMember = new StaffMember
      adminMember.universityId = "admin"
      adminMember.userId = "admin"
      val admin = adminMember.asSsoUser

      val deptAdminMember = new StaffMember
      deptAdminMember.universityId = "admin2"
      deptAdminMember.userId = "admin2"
      val deptAdmin = deptAdminMember.asSsoUser

      val moduleManagerMember = new StaffMember
      moduleManagerMember.universityId = "admin3"
      moduleManagerMember.userId = "admin3"
      val moduleManager = moduleManagerMember.asSsoUser

      userLookup.users = Map("admin" -> admin, "admin2" -> deptAdmin, "admin3" -> moduleManager)
      department.permissionsService = permissionsService
      module.permissionsService = permissionsService

      val assignmentWithParents = Fixtures.withParents(assignment)
      val targetAssignment = assignmentWithParents.head
      val targetModule = assignmentWithParents(1)
      val targetDept = assignmentWithParents(2)
      val targetParentDept = assignmentWithParents(3)

      val moduleGrantedRole = GrantedRole(module, ModuleManagerRoleDefinition)
      moduleGrantedRole.users.add(moduleManager)
      wireUserLookup(moduleGrantedRole.users)

      val deptGrantedRole = GrantedRole(department, DepartmentalAdministratorRoleDefinition)
      deptGrantedRole.users.add(deptAdmin)
      wireUserLookup(deptGrantedRole.users)

      permissionsService.getAllGrantedRolesFor(targetAssignment) returns Nil
      permissionsService.getAllGrantedRolesFor(targetDept) returns Nil
      permissionsService.getAllGrantedRolesFor[PermissionsTarget](targetModule) returns LazyList(moduleGrantedRole).asInstanceOf[LazyList[GrantedRole[PermissionsTarget]]]
      permissionsService.getAllGrantedRolesFor[PermissionsTarget](targetParentDept) returns LazyList(deptGrantedRole).asInstanceOf[LazyList[GrantedRole[PermissionsTarget]]]

      val existing = GrantedPermission(targetDept, Permissions.Submission.Delete, RoleOverride.Allow)
      existing.users.knownType.addUserId("admin3")

      permissionsService.getGrantedPermission(targetAssignment, Permissions.Submission.Delete, RoleOverride.Allow) returns None
      permissionsService.getGrantedPermission(targetDept, Permissions.Submission.Delete, RoleOverride.Allow) returns Some(existing)
      permissionsService.getGrantedPermission(targetModule, Permissions.Submission.Delete, RoleOverride.Allow) returns None
      permissionsService.getGrantedPermission(targetDept, Permissions.Submission.Delete, RoleOverride.Allow) returns None
      permissionsService.getGrantedPermission(targetParentDept, Permissions.Submission.Delete, RoleOverride.Allow) returns None

      val subNotification = new SubmissionReceivedNotification
      subNotification.permissionsService = permissionsService
      subNotification.securityService = securityService

      securityService.can(isA[CurrentUser], isEq(Permissions.Submission.Delete), isA[PermissionsTarget]) returns true

      val settings = new UserSettings("userId")
      subNotification.userSettings = service

      service.getByUserId("admin3") returns None
      service.getByUserId("admin2") returns None

      val n = Notification.init(subNotification, currentUser.apparentUser, submission, assignment)

      n.recipients.size should be(2)
    }
  }

  @Test def lateNotificationUrlsDifferForMarkersAndAdmins(): Unit = withFakeTime(new DateTime(2018, DateTimeConstants.SEPTEMBER, 1, 12, 39, 0, 0)) {
    withUser("cusca", "55556666") {

      val admin: User = Fixtures.user("admin", "admin")
      val marker: User = Fixtures.user("1234567", "1234567")
      val student: User = Fixtures.user("7654321", "7654321")

      val department = Fixtures.department("ch")
      val assignment = Fixtures.assignment("Another 5,000 word essay")
      val module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.module = module
      assignment.closeDate = new DateTime(2017, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      assignment.id = "1234"

      assignment.feedbackService = smartMock[FeedbackService]
      assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

      val workflow = SingleMarkerWorkflow("Test", department, Seq(marker))
      assignment.cm2MarkingWorkflow = workflow

      val submission = Fixtures.submission(userId = "7654321", universityId = "7654321")
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      val mockLookup: UserLookupService = mock[UserLookupService]
      mockLookup.getUserByUserId(marker.getUserId) returns marker
      mockLookup.getUserByUserId(admin.getUserId) returns admin

      val feedback: Feedback = Fixtures.assignmentFeedback(student.getWarwickId)
      assignment.feedbacks.add(feedback)

      val markerFeedback: MarkerFeedback = Fixtures.markerFeedback(feedback)
      markerFeedback.marker = marker
      markerFeedback.stage = SingleMarkingCompleted
      markerFeedback.userLookup = mockLookup

      val n = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)

      n.urlFor(admin) should be("/coursework/admin/assignments/1234/list")
      n.urlFor(marker) should be("/coursework/admin/assignments/1234/marker/1234567")
    }
  }

  @Test def notificationContent(): Unit = withFakeTime(new DateTime(2018, DateTimeConstants.SEPTEMBER, 1, 12, 39, 0, 0)) {
    withUser("cusca", "55556666") {
      val marker: User = Fixtures.user("1234567", "1234567")
      val student: User = Fixtures.user("7654321", "7654321")

      val department = Fixtures.department("ch")
      val assignment = Fixtures.assignment("Another 5,000 word essay")
      val module = Fixtures.module("cs118", "Programming for Computer Scientists")
      assignment.module = module
      assignment.closeDate = new DateTime(2018, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      assignment.id = "1234"

      assignment.feedbackService = smartMock[FeedbackService]
      assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

      val workflow = SingleMarkerWorkflow("Test", department, Seq(marker))
      assignment.cm2MarkingWorkflow = workflow

      val submission = Fixtures.submission(userId = "7654321", universityId = "7654321")
      submission.id = "28334"
      submission.assignment = assignment
      submission.submittedDate = DateTime.now

      val a = new FileAttachment
      a.name = "Essay.docx"
      submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

      val mockLookup: UserLookupService = mock[UserLookupService]
      mockLookup.getUserByUserId(marker.getUserId) returns marker

      val feedback: Feedback = Fixtures.assignmentFeedback(student.getWarwickId)
      assignment.feedbacks.add(feedback)

      val markerFeedback: MarkerFeedback = Fixtures.markerFeedback(feedback)
      markerFeedback.marker = marker
      markerFeedback.stage = SingleMarkingCompleted
      markerFeedback.userLookup = mockLookup

      val n = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
      n.userLookup = mockLookup

      val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
      notificationContent should be(
        """A submission for the assignment 'Another 5,000 word essay' for CS118, Programming for Computer Scientists has been received.
          |
          |- Submission date: 1 September 2018 at 12:39:00
          |- Submission ID: 28334
          |
          |Uploaded attachments:
          |
          |- Essay.docx
          |
          |Student feedback is due for this submission on 12 October 2018.
          |
          |""".stripMargin
      )
    }
  }

  def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
    case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
    case ug: UserGroup => ug.userLookup = userLookup
  }

}
