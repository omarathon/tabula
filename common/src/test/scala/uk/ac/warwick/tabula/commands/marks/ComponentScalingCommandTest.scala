package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ComponentScalingCommandTest extends TestBase with Mockito {

  val scaling =
    new ComponentScalingAlgorithm
      with ComponentScalingRequest
      with RecordAssessmentComponentMarksState
      with AssessmentMembershipServiceComponent {
      override val assessmentComponent: AssessmentComponent = Fixtures.assessmentComponent(Fixtures.module("in101"), 1)
      override val upstreamAssessmentGroup: UpstreamAssessmentGroup = Fixtures.assessmentGroup(assessmentComponent)
      override val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
    }

  @Test
  def defaults(): Unit = {
    scaling.passMarkAdjustment = 5
    scaling.upperClassAdjustment = 5

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
    scaling.passMarkAdjustment = 17
    scaling.upperClassAdjustment = 12

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

}
