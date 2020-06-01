package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, GradeBoundary, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksService, AssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ComponentScalingCommandTest extends TestBase with Mockito {

  val scaling =
    new ComponentScalingAlgorithm
      with ComponentScalingRequest
      with ComponentScalingValidation
      with RecordAssessmentComponentMarksState
      with MissingMarkAdjustmentStudentsToSet
      with AssessmentMembershipServiceComponent
      with AssessmentComponentMarksServiceComponent {
      override val assessmentComponent: AssessmentComponent = Fixtures.assessmentComponent(Fixtures.module("in101"), 1)
      override val upstreamAssessmentGroup: UpstreamAssessmentGroup = Fixtures.assessmentGroupAndMember(assessmentComponent, 70, AcademicYear.now())
      override val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
      override val assessmentComponentMarksService: AssessmentComponentMarksService = smartMock[AssessmentComponentMarksService]

      assessmentComponentMarksService.getAllRecordedStudents(upstreamAssessmentGroup) returns Seq.empty
      calculate = true
    }

  @Test
  def defaults(): Unit = {
    scaling.scaledPassMark = 45
    scaling.scaledUpperClassMark = 75

    scaling.scaleMark(0) should be (0)
    scaling.scaleMark(5) should be (6)
    scaling.scaleMark(10) should be (11)
    scaling.scaleMark(15) should be (17)
    scaling.scaleMark(20) should be (23)
    scaling.scaleMark(25) should be (29)
    scaling.scaleMark(30) should be (34)
    scaling.scaleMark(35) should be (40)
    scaling.scaleMark(40) should be (45)
    scaling.scaleMark(45) should be (50)
    scaling.scaleMark(50) should be (55)
    scaling.scaleMark(55) should be (60)
    scaling.scaleMark(60) should be (65)
    scaling.scaleMark(65) should be (70)
    scaling.scaleMark(70) should be (74)
    scaling.scaleMark(75) should be (79)
    scaling.scaleMark(80) should be (83)
    scaling.scaleMark(85) should be (87)
    scaling.scaleMark(90) should be (91)
    scaling.scaleMark(95) should be (96)
    scaling.scaleMark(100) should be (100)
  }

  @Test
  def options(): Unit = {
    scaling.passMark = 50
    scaling.scaledPassMark = 67
    scaling.scaledUpperClassMark = 82

    scaling.scaleMark(0) should be (0)
    scaling.scaleMark(5) should be (8)
    scaling.scaleMark(10) should be (15)
    scaling.scaleMark(15) should be (23)
    scaling.scaleMark(20) should be (30)
    scaling.scaleMark(25) should be (38)
    scaling.scaleMark(30) should be (45)
    scaling.scaleMark(35) should be (52)
    scaling.scaleMark(40) should be (56)
    scaling.scaleMark(45) should be (60)
    scaling.scaleMark(50) should be (64)
    scaling.scaleMark(55) should be (68)
    scaling.scaleMark(60) should be (71)
    scaling.scaleMark(65) should be (75)
    scaling.scaleMark(70) should be (79)
    scaling.scaleMark(75) should be (82)
    scaling.scaleMark(80) should be (86)
    scaling.scaleMark(85) should be (89)
    scaling.scaleMark(90) should be (93)
    scaling.scaleMark(95) should be (96)
    scaling.scaleMark(100) should be (100)
  }

  @Test
  def validationMissingMarks(): Unit = {
    scaling.upstreamAssessmentGroup.members.get(0).actualMark = None
    scaling.scaledPassMark = 45
    scaling.scaledUpperClassMark = 75

    val errors = new BindException(scaling, "scaling")
    scaling.validate(errors)

    errors.getErrorCount should be (1)
    errors.getGlobalError.getCode should be ("scaling.studentsWithMissingMarks")
    errors.getGlobalError.getArguments should be (Array("0123456"))
  }

  @Test
  def validationNoChanges(): Unit = {
    scaling.upstreamAssessmentGroup.members.get(0).actualGrade = Some(GradeBoundary.WithdrawnGrade)
    scaling.scaledPassMark = 45
    scaling.scaledUpperClassMark = 75

    val errors = new BindException(scaling, "scaling")
    scaling.validate(errors)

    errors.getErrorCount should be (1)
    errors.getGlobalError.getCode should be ("scaling.noChanges")
  }

  @Test
  def validationNoAdjustment(): Unit = {
    val errors = new BindException(scaling, "scaling")
    scaling.validate(errors)

    errors.getErrorCount should be (1)
    errors.getFieldError("scaledUpperClassMark").getCode should be ("scaling.noAdjustments")
  }

  @Test
  def validationPasses(): Unit = {
    scaling.scaledPassMark = 45
    scaling.scaledUpperClassMark = 75

    val errors = new BindException(scaling, "scaling")
    scaling.validate(errors)

    errors.hasErrors should be (false)
  }

  @Test
  def preventsDivByZero(): Unit = {
    def validateInput(passMark: Int, scaledPassMark: Int, scaledUpperClassMark: Int): Boolean = {
      scaling.passMark = passMark
      scaling.scaledPassMark = scaledPassMark
      scaling.scaledUpperClassMark = scaledUpperClassMark

      val errors = new BindException(scaling, "scaling")
      scaling.validate(errors)
      !errors.hasErrors
    }

    // Just hammer it with various options and make sure:
    // - It doesn't blow with a div/0
    // - 0 is 0 and 100 is 100
    // - The scaled marks are contiguous
    for {
      passMark <- Seq(40, 50)
      scaledPassMark <- 0 to 100
      scaledUpperClassMark <- 0 to 100

      if validateInput(passMark, scaledPassMark, scaledUpperClassMark)
    } {
      scaling.scaleMark(0) should be (0)
      scaling.scaleMark(100) should be (100)
      (1 to 99).foreach { mark =>
        val scaled = scaling.scaleMark(mark)
        (scaled >= scaling.scaleMark(mark - 1)) should be (true)
        (scaled <= scaling.scaleMark(mark + 1)) should be (true)
      }
    }
  }

}
