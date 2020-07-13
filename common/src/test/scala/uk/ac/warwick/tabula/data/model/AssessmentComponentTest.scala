package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class AssessmentComponentTest extends TestBase with Mockito {

  val module: Module = Fixtures.module("in101")
  val membershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

  @Test def scaledWeightingForEmptyWeighting(): Unit = {
    // Test that it doesn't blow up if there's no weighting
    val ac = Fixtures.assessmentComponent(module, 1)
    ac.rawWeighting = null
    ac.scaledWeighting should be (empty)
  }

  @Test def scaledWeightingUnscaled(): Unit = {
    // Test that if we have a "normal" layout with weightings that add up to 100, that everything works
    val (a01, a02, e01) = (
      Fixtures.assessmentComponent(module, 1, weighting = 15),
      Fixtures.assessmentComponent(module, 2, weighting = 15),
      Fixtures.assessmentComponent(module, 1, AssessmentType.SummerExam, weighting = 70),
    )

    val components = Seq(a01, a02, e01)
    components.foreach(_.membershipService = membershipService)

    membershipService.getVariableAssessmentWeightingRules("IN101-30", "A") returns Seq.empty
    membershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns components

    components.map(_.scaledWeighting) should be (Seq(Some(BigDecimal(15)), Some(BigDecimal(15)), Some(BigDecimal(70))))
  }

  @Test def scaledWeighting(): Unit = {
    // Raw weightings are 125, 125, 750 to signify 12.5, 12.5, 75
    val (a01, a02, e01) = (
      Fixtures.assessmentComponent(module, 1, weighting = 125),
      Fixtures.assessmentComponent(module, 2, weighting = 125),
      Fixtures.assessmentComponent(module, 1, AssessmentType.SummerExam, weighting = 750),
    )

    val components = Seq(a01, a02, e01)
    components.foreach(_.membershipService = membershipService)

    membershipService.getVariableAssessmentWeightingRules("IN101-30", "A") returns Seq.empty
    membershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns components

    components.map(_.scaledWeighting) should be (Seq(Some(BigDecimal(12.5)), Some(BigDecimal(12.5)), Some(BigDecimal(75))))

    // Also check variableWeighting while we're here, as that shouldn't be affected when there's no rules but should still scale
    components.map(_.weightingFor(Seq.empty).get) should be (Seq(Some(BigDecimal(12.5)), Some(BigDecimal(12.5)), Some(BigDecimal(75))))
  }

  @Test def variableWeighting(): Unit = {
    // Weightings are (30, 30, 70) because only the higher marked essay will count
    val (a01, a02, e01) = (
      Fixtures.assessmentComponent(module, 1, weighting = 30),
      Fixtures.assessmentComponent(module, 2, weighting = 30),
      Fixtures.assessmentComponent(module, 1, AssessmentType.SummerExam, weighting = 70),
    )

    val components = Seq(a01, a02, e01)
    components.foreach(_.membershipService = membershipService)

    val rules = Seq(
      Fixtures.variableAssessmentWeightingRule(module, 1, weighting = 30),
      Fixtures.variableAssessmentWeightingRule(module, 2, weighting = 0),
      Fixtures.variableAssessmentWeightingRule(module, 3, assessmentType = AssessmentType.SummerExam, weighting = 70),
    )

    membershipService.getVariableAssessmentWeightingRules("IN101-30", "A") returns rules
    membershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns components

    val marks = Seq(
      (AssessmentType.Essay, "A01", Some(75)),
      (AssessmentType.Essay, "A02", Some(60)),
      (AssessmentType.SummerExam, "E01", None), // It doesn't matter that the exam hasn't happened yet
    )
    components.map(_.weightingFor(marks).get) should be (Seq(Some(BigDecimal(30)), Some(BigDecimal(0)), Some(BigDecimal(70))))

    // If the marks are equal, the first by sequence takes precedence
    val marks2 = Seq(
      (AssessmentType.Essay, "A01", Some(60)),
      (AssessmentType.Essay, "A02", Some(60)),
      (AssessmentType.SummerExam, "E01", None),
    )
    components.map(_.weightingFor(marks2).get) should be (Seq(Some(BigDecimal(30)), Some(BigDecimal(0)), Some(BigDecimal(70))))

    // Check that it works if A02 has a higher mark
    val marks3 = Seq(
      (AssessmentType.Essay, "A01", Some(60)),
      (AssessmentType.Essay, "A02", Some(75)),
      (AssessmentType.SummerExam, "E01", None),
    )
    components.map(_.weightingFor(marks3).get) should be (Seq(Some(BigDecimal(0)), Some(BigDecimal(30)), Some(BigDecimal(70))))

    // Check that a missing mark is treated correctly
    val marks4 = Seq(
      (AssessmentType.Essay, "A01", None),
      (AssessmentType.Essay, "A02", Some(75)),
      (AssessmentType.SummerExam, "E01", None),
    )
    components.map(_.weightingFor(marks4).get) should be (Seq(Some(BigDecimal(0)), Some(BigDecimal(30)), Some(BigDecimal(70))))
  }

  @Test def scaledVariableWeighting(): Unit = {
    // If anyone ever does this they're going to hell. Highest mark is worth 75%, next two are worth 12.5% each, fourth is worth 0%
    val (a01, a02, a03, a04) = (
      Fixtures.assessmentComponent(module, 1, weighting = 25),
      Fixtures.assessmentComponent(module, 2, weighting = 25),
      Fixtures.assessmentComponent(module, 3, weighting = 25),
      Fixtures.assessmentComponent(module, 4, weighting = 25),
    )

    val components = Seq(a01, a02, a03, a04)
    components.foreach(_.membershipService = membershipService)

    val rules = Seq(
      Fixtures.variableAssessmentWeightingRule(module, 1, weighting = 750),
      Fixtures.variableAssessmentWeightingRule(module, 2, weighting = 125),
      Fixtures.variableAssessmentWeightingRule(module, 3, weighting = 125),
      Fixtures.variableAssessmentWeightingRule(module, 4, weighting = 0),
    )

    membershipService.getVariableAssessmentWeightingRules("IN101-30", "A") returns rules
    membershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns components

    // A02 75%, A04 12.5%, A01 12.5%, A03 0%
    val marks = Seq(
      (AssessmentType.Essay, "A01", Some(50)),
      (AssessmentType.Essay, "A02", Some(88)),
      (AssessmentType.Essay, "A03", Some(19)),
      (AssessmentType.Essay, "A04", Some(53)),
    )
    components.map(_.weightingFor(marks).get) should be (Seq(Some(BigDecimal(12.5)), Some(BigDecimal(75)), Some(BigDecimal(0)), Some(BigDecimal(12.5))))
  }

}
