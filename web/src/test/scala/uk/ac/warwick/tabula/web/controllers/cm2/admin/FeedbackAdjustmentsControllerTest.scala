package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.feedback.{FeedbackAdjustmentCommandState, FeedbackAdjustmentGradeValidation}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.services.{ExtensionService, GeneratesGradesFromMarks, ProfileService, ValidateAndPopulateFeedbackResult}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

class FeedbackAdjustmentsControllerTest extends TestBase with Mockito {

  private trait ControllerFixture {
    val controller = new FeedbackAdjustmentsController
    controller.profileService = smartMock[ProfileService]
  }

  private trait CommandFixture {
    val assignment: Assignment = Fixtures.assignment("5,000 word essay")
    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

    assignment.module = Fixtures.module("cs118")
    assignment.module.adminDepartment = Fixtures.department("cs")

    val submission: Submission = Fixtures.submission("1234567", "1234567").tap(_.assignment = assignment)
    val feedback: Feedback = Fixtures.assignmentFeedback("1234567", "1234567").tap { f =>
      f.assignment = assignment
      f.actualMark = Some(50)
    }

    assignment.submissions.add(submission)
    val thisStudent = new User("1234567")
    thisStudent.setWarwickId("1234567")

    val command = new Appliable[Feedback] with FeedbackAdjustmentCommandState with FeedbackAdjustmentGradeValidation {
      override def apply(): Feedback = {
        null
      }

      override val gradeGenerator: GeneratesGradesFromMarks = mock[GeneratesGradesFromMarks]
      override val student: User = thisStudent
      override val submitter: CurrentUser = null

      override val assessment: Assignment = CommandFixture.this.assignment
      override val feedback: Feedback = CommandFixture.this.feedback

      override val gradeValidation: Option[ValidateAndPopulateFeedbackResult] = None
    }
  }

  private trait UGStudentFixture extends ControllerFixture with CommandFixture {
    val ugStudent: StudentMember = Fixtures.student("1234567")
    ugStudent.mostSignificantCourse.course = Fixtures.course("U100-ABCD")

    controller.profileService.getMemberByUniversityId("1234567") returns Some(ugStudent)
  }

  private trait PGStudentFixture extends ControllerFixture with CommandFixture {
    val pgStudent: StudentMember = Fixtures.student("1234567")
    pgStudent.mostSignificantCourse.course = Fixtures.course("TESA-H64A")

    controller.profileService.getMemberByUniversityId("1234567") returns Some(pgStudent)
  }

  private trait FoundationStudentFixture extends ControllerFixture with CommandFixture {
    val foundationStudent: StudentMember = Fixtures.student("1234567")
    foundationStudent.mostSignificantCourse.course = Fixtures.course("FFFF-FFFF")

    controller.profileService.getMemberByUniversityId("1234567") returns Some(foundationStudent)
  }

  private trait NoStudentFoundFixture extends ControllerFixture with CommandFixture {
    controller.profileService.getMemberByUniversityId("1234567") returns None
  }

  @Test def ugPenalty(): Unit = {
    new UGStudentFixture {
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 15, 0, 0, 0)

      val mav: Mav = controller.showForm(command, assignment, thisStudent)
      mav.viewName should be("cm2/admin/assignments/feedback/adjustments")
      mav.toModel("daysLate") should be(Some(2))
      mav.toModel("marksSubtracted") should be(Some(10))
      mav.toModel("proposedAdjustment") should be(Some(40))
      mav.toModel("latePenalty") should be(5)
    }
  }

  @Test def pgPenaltyPre2019(): Unit = {
    new PGStudentFixture {
      pgStudent.mostSignificantCourse.sprStartAcademicYear = AcademicYear.starting(2012)

      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 15, 0, 0, 0)

      val mav: Mav = controller.showForm(command, assignment, thisStudent)
      mav.viewName should be("cm2/admin/assignments/feedback/adjustments")
      mav.toModel("daysLate") should be(Some(2))
      mav.toModel("marksSubtracted") should be(Some(6))
      mav.toModel("proposedAdjustment") should be(Some(44))
      mav.toModel("latePenalty") should be(3)
    }
  }

  @Test def pgPenaltyPost2019(): Unit = {
    new PGStudentFixture {
      pgStudent.mostSignificantCourse.sprStartAcademicYear = AcademicYear.starting(2019)

      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 15, 0, 0, 0)

      val mav: Mav = controller.showForm(command, assignment, thisStudent)
      mav.viewName should be("cm2/admin/assignments/feedback/adjustments")
      mav.toModel("daysLate") should be(Some(2))
      mav.toModel("marksSubtracted") should be(Some(10))
      mav.toModel("proposedAdjustment") should be(Some(40))
      mav.toModel("latePenalty") should be(5)
    }
  }

  @Test def openEndedNoProposedPenalty(): Unit = {
    new PGStudentFixture {
      submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 15, 0, 0, 0)
      assignment.openEnded = true

      val mav: Mav = controller.showForm(command, assignment, thisStudent)
      mav.viewName should be("cm2/admin/assignments/feedback/adjustments")
      mav.toModel("daysLate") should be(Some(0))
      mav.toModel("marksSubtracted") should be(Some(0))
      mav.toModel("proposedAdjustment") should be(None)
      mav.toModel("latePenalty") should be(5)
    }
  }

  @Test def foundationCoursePenalty(): Unit = {
    new FoundationStudentFixture {
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 15, 0, 0, 0)

      val mav: Mav = controller.showForm(command, assignment, thisStudent)
      mav.viewName should be("cm2/admin/assignments/feedback/adjustments")
      mav.toModel("daysLate") should be(Some(2))
      mav.toModel("marksSubtracted") should be(Some(10))
      mav.toModel("proposedAdjustment") should be(Some(40))
      mav.toModel("latePenalty") should be(5)
    }
  }

  @Test def notFoundCoursePenalty(): Unit = {
    new NoStudentFoundFixture {
      assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
      submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 15, 0, 0, 0)

      val mav: Mav = controller.showForm(command, assignment, thisStudent)
      mav.viewName should be("cm2/admin/assignments/feedback/adjustments")
      mav.toModel("daysLate") should be(Some(2))
      mav.toModel("marksSubtracted") should be(Some(10))
      mav.toModel("proposedAdjustment") should be(Some(40))
      mav.toModel("latePenalty") should be(5)
    }
  }

}
