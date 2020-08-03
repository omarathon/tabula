package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ModuleRegistrationTest extends TestBase with Mockito {
  @Test def selectionStatus(): Unit = {
    val stuMem = new StudentMember("0123456")
    stuMem.userId = "abcde"

    val scd: StudentCourseDetails = new StudentCourseDetails(stuMem, "0123456/1")

    val module = new Module
    module.code = "ab123"

    val modReg = new ModuleRegistration(scd.sprCode, module, new JBigDecimal("10"), "AB123-10", AcademicYear(2012), "A", null)
    modReg.assessmentGroup = "D"
    modReg.selectionStatus = ModuleSelectionStatus.OptionalCore

    modReg.selectionStatus.description should be("O Core")

    modReg.selectionStatus = ModuleSelectionStatus.fromCode("C")

    modReg.selectionStatus.description should be("Core")

    modReg.passFail should be (false)
    modReg.marksCode = "WAR"
    modReg.passFail should be (false)
    modReg.marksCode = "PF"
    modReg.passFail should be (true)
  }

  @Test def upstreamAssessmentGroupMembersAllAttempts(): Unit = {
    val student = Fixtures.student(universityId = "8347243")
    val scd = student.mostSignificantCourse
    val academicYear = scd.latestStudentCourseYearDetails.academicYear

    val module = Fixtures.module("in101")

    val moduleRegistration = new ModuleRegistration(scd.sprCode, module, BigDecimal(30).underlying, "IN101-30", academicYear, "A", "WMR")
    moduleRegistration._allStudentCourseDetails.add(scd)
    moduleRegistration.assessmentGroup = "A1"
    moduleRegistration.selectionStatus = ModuleSelectionStatus.OptionalCore

    // Original assessment pattern
    val a01 = Fixtures.assessmentComponent(module, 1, assessmentType = AssessmentType.Essay, weighting = 30, assessmentGroup = "A1", marksCode = "WAR")
    val uagA01 = Fixtures.assessmentGroup(a01, academicYear)

    val e01 = Fixtures.assessmentComponent(module, 2, assessmentType = AssessmentType.SummerExam, weighting = 70, assessmentGroup = "A1", marksCode = "WAR")
    val uagE01 = Fixtures.assessmentGroup(e01, academicYear)

    // Re-assessment pattern
    val e02 = Fixtures.assessmentComponent(module, 3, assessmentType = AssessmentType.SummerExam, weighting = 100, assessmentGroup = "RA1", marksCode = "WAR")
    val uagE02 = Fixtures.assessmentGroup(e02, academicYear)

    val membershipService = smartMock[AssessmentMembershipService]
    membershipService.getUpstreamAssessmentGroups(moduleRegistration, allAssessmentGroups = true, eagerLoad = true) returns Seq(uagA01, uagE01, uagE02)
    membershipService.getVariableAssessmentWeightingRules(anyString, anyString) returns Seq.empty
    membershipService.getAssessmentComponents("IN101-30", inUseOnly = false) returns Seq(a01, e01, e02)

    moduleRegistration.membershipService = membershipService
    a01.membershipService = membershipService
    e01.membershipService = membershipService
    e02.membershipService = membershipService

    // Attempt 1: 30% assignment, 70% exam
    val sitting1UAGMA01 = new UpstreamAssessmentGroupMember(uagA01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment).tap(_.id = "sitting1UAGMA01")
    val sitting1UAGME01 = new UpstreamAssessmentGroupMember(uagE01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment).tap(_.id = "sitting1UAGME01")

    uagA01.members.add(sitting1UAGMA01)
    uagE01.members.add(sitting1UAGME01)

    // Attempt 2: 70% exam, FFA (using original assignment mark)
    val sitting2UAGME01 =
      new UpstreamAssessmentGroupMember(uagE01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("001"))
        .tap { resit =>
          resit.id = "sitting2UAGME01"
          resit.currentResitAttempt = Some(1)
          resit.resitAssessmentName = Some("Exam (resit)")
          resit.resitAssessmentType = Some(AssessmentType.SeptemberExam)
          // Just let it pick up the original assessment weighting
        }

    uagE01.members.add(sitting2UAGME01)

    // Attempt 3: 100% exam, FFA (by the same assessment pattern)
    val sitting3UAGME01 =
      new UpstreamAssessmentGroupMember(uagE01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("002"))
        .tap { resit =>
          resit.id = "sitting3UAGME01"
          resit.currentResitAttempt = Some(1)
          resit.resitAssessmentName = Some("Exam (resit)")
          resit.resitAssessmentType = Some(AssessmentType.SummerExam)
          resit.resitAssessmentWeighting = Some(100)
        }

    uagE01.members.add(sitting3UAGME01)

    // Attempt 4: 100% exam, capped (by the same assessment pattern)
    val sitting4UAGME01 =
      new UpstreamAssessmentGroupMember(uagE01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("003"))
        .tap { resit =>
          resit.id = "sitting4UAGME01"
          resit.currentResitAttempt = Some(2)
          resit.resitAssessmentName = Some("Exam (resit)")
          resit.resitAssessmentType = Some(AssessmentType.SeptemberExam)
          resit.resitAssessmentWeighting = Some(100)
        }

    uagE01.members.add(sitting4UAGME01)

    // Attempt 5: 100% exam, capped (by alternative assessment pattern)
    val sitting5UAGME02 =
      new UpstreamAssessmentGroupMember(uagE02, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("004"))
        .tap { resit =>
          resit.id = "sitting5UAGME02"
          resit.currentResitAttempt = Some(2)
          resit.resitAssessmentType = Some(AssessmentType.SummerExam)
        }

    uagE02.members.add(sitting5UAGME02)

    // Attempt 6: 50% assignemnt, 50% exam (by original assessment pattern)
    val sitting6UAGMA01 =
      new UpstreamAssessmentGroupMember(uagA01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("005"))
        .tap { resit =>
          resit.id = "sitting6UAGMA01"
          resit.currentResitAttempt = Some(2)
          resit.resitAssessmentName = Some("Assignment (resit)")
          resit.resitAssessmentWeighting = Some(50)
        }

    val sitting6UAGME01 =
      new UpstreamAssessmentGroupMember(uagE01, student.universityId, UpstreamAssessmentGroupMemberAssessmentType.Reassessment, Some("006"))
        .tap { resit =>
          resit.id = "sitting6UAGME01"
          resit.currentResitAttempt = Some(2)
          resit.resitAssessmentName = Some("Exam (resit)")
          resit.resitAssessmentType = Some(AssessmentType.SeptemberExam)
          resit.resitAssessmentWeighting = Some(50)
        }

    uagA01.members.add(sitting6UAGMA01)
    uagE01.members.add(sitting6UAGME01)

    def extractMarks(components: Seq[UpstreamAssessmentGroupMember]): Seq[(AssessmentType, String, Option[Int])] = components.flatMap { uagm =>
      uagm.upstreamAssessmentGroup.assessmentComponent.map { ac =>
        (ac.assessmentType, ac.sequence, uagm.firstDefinedMark)
      }
    }

    val sittings = moduleRegistration.upstreamAssessmentGroupMembersAllAttempts(extractMarks)
    sittings.size should be (6)

    sittings match {
      case Seq(sitting1, sitting2, sitting3, sitting4, sitting5, sitting6) =>
        // Don't want to .toSet here as want to catch accidental dupes
        sitting1.sortBy(_._1.id) should be (Seq(sitting1UAGMA01 -> Some(BigDecimal(30)), sitting1UAGME01 -> Some(BigDecimal(70))))
        sitting2.sortBy(_._1.id) should be (Seq(sitting1UAGMA01 -> Some(BigDecimal(30)), sitting2UAGME01 -> Some(BigDecimal(70))))
        sitting3.sortBy(_._1.id) should be (Seq(sitting3UAGME01 -> Some(BigDecimal(100))))
        sitting4.sortBy(_._1.id) should be (Seq(sitting4UAGME01 -> Some(BigDecimal(100))))
        sitting5.sortBy(_._1.id) should be (Seq(sitting5UAGME02 -> Some(BigDecimal(100))))
        sitting6.sortBy(_._1.id) should be (Seq(sitting6UAGMA01 -> Some(BigDecimal(50)), sitting6UAGME01 -> Some(BigDecimal(50))))
    }
  }
}
