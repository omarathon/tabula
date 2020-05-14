package uk.ac.warwick.tabula.services.scheduling

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ExportFeedbackToSitsServiceTest extends TestBase with Mockito {

  trait Environment {
    val year = AcademicYear(2014)

    val module: Module = Fixtures.module("nl901", "Foraging Forays")

    val assignment: Assignment = Fixtures.assignment("Your challenge, should you choose to accept it")
    assignment.academicYear = year
    assignment.module = module
    assignment.assessmentGroups.add({
      val group = new AssessmentGroup
      group.assignment = assignment
      group.occurrence = "B"
      group.assessmentComponent = Fixtures.assessmentComponent(Fixtures.module("nl901"), 2)
      group.membershipService = smartMock[AssessmentMembershipService]
      group.membershipService.getUpstreamAssessmentGroupInfo(any[UpstreamAssessmentGroup]) returns Some(
        Fixtures.upstreamAssessmentGroupInfo(year, "A", "NL901-30", "B")
      )
      group
    })

    val feedback: Feedback = Fixtures.assignmentFeedback("1000006")
    feedback.assignment = assignment

    val feedbackForSits: FeedbackForSits = Fixtures.feedbackForSits(feedback, currentUser.apparentUser)

    val paramGetter = new FeedbackParameterGetter(feedback)

  }

  @Test
  def queryParams(): Unit = withUser("1000006", "cusdx") {
    new Environment {
      val inspectMe: JMap[String, Any] = paramGetter.getQueryParams.get
      inspectMe.get("studentId") should be("1000006")
      inspectMe.get("academicYear") should be(year.toString)
      inspectMe.get("moduleCodeMatcher") should be("NL901%")
    }
  }

  @Test
  def noAssessmentGroups(): Unit = withUser("1000006", "cusdx") {
    new Environment {
      assignment.assessmentGroups.clear()
      val newParamGetter = new FeedbackParameterGetter(feedback)
      val inspectMe: Option[JMap[String, Any]] = newParamGetter.getQueryParams
      inspectMe.isEmpty should be(true)
    }
  }

  @Test
  def updateParams(): Unit = withUser("1000006", "cusdx") {
    new Environment {

      val inspectMe: JMap[String, Any] = paramGetter.getUpdateParams(73, "A").get
      inspectMe.get("studentId") should be("1000006")
      inspectMe.get("academicYear") should be(year.toString)
      inspectMe.get("moduleCodeMatcher") should be("NL901%")
      inspectMe.get("actualMark", 73)
      inspectMe.get("actualGrade", "A")
    }
  }

}
