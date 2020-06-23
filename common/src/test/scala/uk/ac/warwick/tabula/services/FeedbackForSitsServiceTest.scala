package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.{FeedbackForSitsDao, FeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime

class FeedbackForSitsServiceTest extends TestBase with Mockito {

  trait ServiceTestSupport extends FeedbackForSitsDaoComponent {
    val feedbackForSitsDao: FeedbackForSitsDao = smartMock[FeedbackForSitsDao]
  }

  trait Fixture {
    val service = new AbstractFeedbackForSitsService with ServiceTestSupport
    val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
    service.assessmentMembershipService = assessmentMembershipService
    val module = new Module
    val feedback: Feedback = Fixtures.assignmentFeedback("0123456") // matches a member in Fixtures.assessmentGroup
    feedback.assignment = new Assignment
    feedback.assignment.module = module
    feedback.assignment.module.adminDepartment = Fixtures.department("xx")
    feedback.assignment.module.adminDepartment.assignmentGradeValidation = true
    feedback.actualMark = Some(100)

    val upstreamAssesmentGroupInfo: UpstreamAssessmentGroupInfo = Fixtures.upstreamAssessmentGroupInfo(AcademicYear(2010), "A", module.code, "A", "A01")
    assessmentMembershipService.getUpstreamAssessmentGroupInfo(any[UpstreamAssessmentGroup]) returns Option(upstreamAssesmentGroupInfo)

    val upstream = new AssessmentComponent
    upstream.module = module
    upstream.moduleCode = upstreamAssesmentGroupInfo.upstreamAssessmentGroup.moduleCode
    upstream.sequence = upstreamAssesmentGroupInfo.upstreamAssessmentGroup.sequence
    upstream.assessmentGroup = upstreamAssesmentGroupInfo.upstreamAssessmentGroup.assessmentGroup
    upstream.assessmentType = AssessmentType.Essay
    upstream.name = "Egg plants"
    upstream.inUse = true

    val ag = new AssessmentGroup
    ag.membershipService = assessmentMembershipService
    ag.assessmentComponent = upstream
    ag.occurrence = upstreamAssesmentGroupInfo.upstreamAssessmentGroup.occurrence
    ag.assignment = feedback.assignment
    feedback.assignment.assessmentGroups.add(ag)

    val submitter: CurrentUser = currentUser
    val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
    gradeGenerator.applyForMarks(Map(feedback._universityId -> feedback.actualMark.get)) returns Map(feedback._universityId -> Seq(GradeBoundary(null, null, 1, 0, "A", Some(0), Some(100), "N", None)))
  }

  @Test
  def queueNewFeedbackForSits() = withUser("abcde") {
    new Fixture {
      service.getByFeedback(feedback) returns None
      val feedbackForSits: FeedbackForSits = service.queueFeedback(feedback, submitter, gradeGenerator).get

      feedbackForSits.feedback should be(feedback)
      feedbackForSits.initialiser should be(currentUser.apparentUser)
      feedbackForSits.actualGradeLastUploaded should be(null)
      feedbackForSits.actualMarkLastUploaded should be(null)
    }
  }

  @Test
  def queueExistingFeedbackForSits() = withUser("abcde") {
    new Fixture {
      val existingFeedbackForSits = new FeedbackForSits
      val grade = "B"
      val mark = 72
      val firstCreatedDate: DateTime = new DateTime().minusWeeks(2)
      existingFeedbackForSits.actualGradeLastUploaded = grade
      existingFeedbackForSits.actualMarkLastUploaded = mark
      existingFeedbackForSits.initialiser = new User("cuscao")
      existingFeedbackForSits.firstCreatedOn = firstCreatedDate
      existingFeedbackForSits.lastInitialisedOn = firstCreatedDate
      service.getByFeedback(feedback) returns Some(existingFeedbackForSits)

      val feedbackForSits: FeedbackForSits = service.queueFeedback(feedback, submitter, gradeGenerator).get

      feedbackForSits.feedback should be(feedback)
      feedbackForSits.initialiser should be(currentUser.apparentUser)
      feedbackForSits.lastInitialisedOn should not be firstCreatedDate
      feedbackForSits.firstCreatedOn should be(firstCreatedDate)
      feedbackForSits.actualGradeLastUploaded should be(grade)
      feedbackForSits.actualMarkLastUploaded should be(mark)
    }
  }
}
