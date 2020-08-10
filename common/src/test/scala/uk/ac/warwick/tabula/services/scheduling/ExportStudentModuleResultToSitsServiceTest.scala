package uk.ac.warwick.tabula.services.scheduling

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.GradeBoundarySignalStatus.NoSignal
import uk.ac.warwick.tabula.data.model.MarkState._
import uk.ac.warwick.tabula.data.model.ModuleResult._
import uk.ac.warwick.tabula.data.model.RecordedModuleMarkSource._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.services.scheduling.ExportStudentModuleResultToSitsService.SmrSubset

class ExportStudentModuleResultToSitsServiceTest extends PersistenceTestBase  with Mockito with EmbeddedSits {

  val smrExporter = new ExportStudentModuleResultToSitsServiceImpl
  smrExporter.sitsDataSource = sits
  smrExporter.assessmentMembershipService =  smartMock[AssessmentMembershipService]
  ExportStudentModuleResultToSitsService.sitsSchema = "public"

  val thisDepartment: Department = Fixtures.department("ch")
  val thisAcademicYear: AcademicYear = AcademicYear(2018)
  val module1: Module = Fixtures.module("ch118")
  val scd: StudentCourseDetails = Fixtures.student("0123401").mostSignificantCourse
  scd.levelCode = "3"

  @Test def smrExportWithNoSmo(): Unit = {
    val mr1: ModuleRegistration = Fixtures.moduleRegistration(scd, module1, JBigDecimal(Some(30)), AcademicYear.starting(2018), "A1")
    val rmr = new RecordedModuleRegistration(mr1)
    smrExporter.smoRecordExists(rmr) should be (false)
  }

  @Test def smrExportWithNoExistingPriorSMR(): Unit = {
    val mr1: ModuleRegistration = Fixtures.moduleRegistration(scd, module1, JBigDecimal(Some(30)), AcademicYear.starting(2018), "A")
    val rmr = new RecordedModuleRegistration(mr1)
    smrExporter.smoRecordExists(rmr) should be (true)
    smrExporter.smrRecordSubdata(rmr) should be (empty)
  }

  trait ExportSMREnvironment {
    val stu1: StudentMember = Fixtures.student(universityId = "0123451", userId = "student")
    val stu2: StudentMember = Fixtures.student(universityId = "0123453", userId = "student")
    val stu3: StudentMember = Fixtures.student(universityId = "0123452", userId = "student")
    val stu4: StudentMember = Fixtures.student(universityId = "0123461", userId = "student")
    session.saveOrUpdate(stu1)
    session.saveOrUpdate(stu2)
    session.saveOrUpdate(stu3)
    session.saveOrUpdate(stu4)

    val scd1: StudentCourseDetails = stu1.mostSignificantCourseDetails.get
    val scd2: StudentCourseDetails = stu2.mostSignificantCourseDetails.get
    val scd3: StudentCourseDetails = stu3.mostSignificantCourseDetails.get
    val scd4: StudentCourseDetails = stu4.mostSignificantCourseDetails.get
    session.saveOrUpdate(scd1)
    session.saveOrUpdate(scd2)
    session.saveOrUpdate(scd3)
    session.saveOrUpdate(scd4)

    val mod: Module = Fixtures.module("ch118", "Nuclear")
    session.saveOrUpdate(mod)
    session.flush()

    val mr1 = new ModuleRegistration(scd1.sprCode, mod, new JBigDecimal(30), "CH118-30", AcademicYear(2018), "A", null)
    val mr2 = new ModuleRegistration(scd2.sprCode, mod, new JBigDecimal(30), "CH118-30", AcademicYear(2018), "A", "21")
    val mr3 = new ModuleRegistration(scd3.sprCode, mod, new JBigDecimal(30), "CH118-30", AcademicYear(2018), "A", "21")
    val mr4 = new ModuleRegistration(scd4.sprCode, mod, new JBigDecimal(30), "CH118-30", AcademicYear(2018), "A", "F")
    session.saveOrUpdate(mr1)
    session.saveOrUpdate(mr2)
    session.saveOrUpdate(mr3)
    session.saveOrUpdate(mr4)

    val rmr1 = new RecordedModuleRegistration(mr1)
    val rmr2 = new RecordedModuleRegistration(mr2)
    val rmr3 = new RecordedModuleRegistration(mr3)
    val rmr4 = new RecordedModuleRegistration(mr4)

    session.saveOrUpdate(rmr1)
    session.saveOrUpdate(rmr2)
    session.saveOrUpdate(rmr3)
    session.saveOrUpdate(rmr4)
    session.flush()
    session.clear()
    val dbRmr1: RecordedModuleRegistration = session.get(classOf[RecordedModuleRegistration].getName, rmr1.id).asInstanceOf[RecordedModuleRegistration]
    val rMark1: RecordedModuleMark = dbRmr1.addMark( uploader = currentUser.apparentUser,
      mark = Some(40),
      grade = Some("2"),
      result = Some(Pass),
      source = ComponentMarkCalculation,
      markState = UnconfirmedActual,
      comments = "Module mark entry" )
    session.saveOrUpdate(rMark1)
    smrExporter.assessmentMembershipService.gradesForMark(mr1, Some(40)) returns Nil

    val dbRmr2: RecordedModuleRegistration = session.get(classOf[RecordedModuleRegistration].getName, rmr2.id).asInstanceOf[RecordedModuleRegistration]
    val rMark2: RecordedModuleMark = dbRmr2.addMark( uploader = currentUser.apparentUser,
      mark = None,
      grade = None,
      result = None,
      source = ComponentMarkCalculation,
      markState = UnconfirmedActual,
      comments = "Module mark reset" )
    session.saveOrUpdate(rMark2)
    smrExporter.assessmentMembershipService.gradesForMark(mr2, None) returns Nil

    val dbRmr3: RecordedModuleRegistration = session.get(classOf[RecordedModuleRegistration].getName, rmr3.id).asInstanceOf[RecordedModuleRegistration]

    val rMark3Actual: RecordedModuleMark = dbRmr3.addMark(uploader = currentUser.apparentUser,
      mark = Some(61),
      grade = Some("21"),
      result = Some(Pass),
      source = ComponentMarkCalculation,
      markState = ConfirmedActual,
      comments = "Actual marks")
    session.saveOrUpdate(rMark3Actual)
    smrExporter.assessmentMembershipService.gradesForMark(mr3, Some(61)) returns Nil

    val rMark3: RecordedModuleMark = dbRmr3.addMark(uploader = currentUser.apparentUser,
      mark = Some(62),
      grade = Some("21"),
      result = Some(Pass),
      source = ComponentMarkCalculation,
      markState = Agreed,
      comments = "Agreed marks")
    session.saveOrUpdate(rMark3)
  }

  @Transactional
  @Test def smrExportActualPassMarksWithNoPriorResits(): Unit = {
    withUser("cusxxx") {
      new ExportSMREnvironment {
        val m1: ModuleRegistration = dbRmr1.moduleRegistration.get
        m1.membershipService = smartMock[AssessmentMembershipService]
        m1.membershipService.getUpstreamAssessmentGroups(m1, allAssessmentGroups = true, eagerLoad = true) returns Nil

        smrExporter.smoRecordExists(dbRmr1) should be (true)
        smrExporter.smrRecordSubdata(dbRmr1) should be (defined)

        val cnt: Int = smrExporter.exportModuleMarksToSits(dbRmr1, finalAssessmentAttended = false)
        session.flush()
        session.clear()
        cnt should be(1)
        val existingSmr: Option[SmrSubset] = smrExporter.smrRecordSubdata(dbRmr1)
        existingSmr.size should be(1)
        existingSmr.get.actualMark should be(Some(40))
        existingSmr.get.actualGrade should be(Some("2"))
        existingSmr.get.agreedMark should be(None)
        existingSmr.get.agreedGrade should be(None)
        existingSmr.get.currentAttempt should be(Some(1))
        existingSmr.get.completedAttempt should be(Some(0))
        existingSmr.get.sasStatus should be(None)
        existingSmr.get.processStatus should be(Some("C"))
        existingSmr.get.process should be(Some("SAS"))
      }
    }
  }


  @Transactional
  @Test def smrExportWithBlankRecordedMark(): Unit = {
    //if we move back from agreed mark to start actual this should clear both agreed/actual marks and leave other fields as expected
    withUser("cusxxx") {
      new ExportSMREnvironment {
        val m2: ModuleRegistration = dbRmr2.moduleRegistration.get
        m2.membershipService = smartMock[AssessmentMembershipService]
        m2.membershipService.getUpstreamAssessmentGroups(m2, allAssessmentGroups = true, eagerLoad = true) returns Nil

        smrExporter.smoRecordExists(dbRmr2) should be (true)
        smrExporter.smrRecordSubdata(dbRmr2) should be (defined)

        //pick up some student with agreed/actual marks and then export with blank mark entry
        val cnt: Int = smrExporter.exportModuleMarksToSits(dbRmr2, finalAssessmentAttended = false)
        session.flush()
        session.clear()
        cnt should be(1)
        val existingSmr: Option[SmrSubset] = smrExporter.smrRecordSubdata(dbRmr2)
        existingSmr.size should be(1)
        existingSmr.get.actualMark should be(None)
        existingSmr.get.actualGrade should be(None)
        existingSmr.get.agreedMark should be(None)
        existingSmr.get.agreedGrade should be(None)
        existingSmr.get.currentAttempt should be(Some(1))
        existingSmr.get.completedAttempt should be(Some(0))
        existingSmr.get.sasStatus should be(None)
        existingSmr.get.processStatus should be(None)
        existingSmr.get.process should be(Some("SAS"))
      }
    }
  }


  @Transactional
  @Test def smrExportWithAgreedRecordedMark(): Unit = {
    withUser("cusxxx") {
      new ExportSMREnvironment {
        val m1: ModuleRegistration = dbRmr3.moduleRegistration.get
        m1.membershipService = smartMock[AssessmentMembershipService]
        m1.membershipService.getUpstreamAssessmentGroups(m1, allAssessmentGroups = true, eagerLoad = true) returns Nil
        smrExporter.assessmentMembershipService.gradesForMark(m1, Some(62)) returns Nil

        smrExporter.smoRecordExists(dbRmr3) should be (true)
        smrExporter.smrRecordSubdata(dbRmr3) should be (defined)

        val cnt: Int = smrExporter.exportModuleMarksToSits(dbRmr3, finalAssessmentAttended = false)
        session.flush()
        session.clear()
        cnt should be(1)
        val existingSmr: Option[SmrSubset] = smrExporter.smrRecordSubdata(dbRmr3)
        existingSmr.size should be(1)
        existingSmr.get.actualMark should be(Some(61))
        existingSmr.get.actualGrade should be(Some("21"))
        existingSmr.get.agreedMark should be(Some(62))
        existingSmr.get.agreedGrade should be(Some("21"))
        existingSmr.get.currentAttempt should be(Some(1))
        existingSmr.get.completedAttempt should be(Some(1))
        existingSmr.get.sasStatus should be(Some("A"))
        existingSmr.get.processStatus should be(Some("A"))
        existingSmr.get.process should be(Some("COM"))
      }
    }

  }

  @Transactional
  @Test def smrExportWithAgreedFailedRecordedMark(): Unit = {
    withUser("cusxxx") {
      new ExportSMREnvironment {
        val dbRmr4: RecordedModuleRegistration = session.get(classOf[RecordedModuleRegistration].getName, rmr4.id).asInstanceOf[RecordedModuleRegistration]
        val rMark4: RecordedModuleMark = dbRmr4.addMark(uploader = currentUser.apparentUser,
          mark = Some(20),
          grade = Some("R"),
          result = Some(Fail),
          source = ComponentMarkCalculation,
          markState = Agreed,
          comments = "Agreed fail")
        smrExporter.assessmentMembershipService.gradesForMark(mr4, Some(20)) returns Seq(GradeBoundary (
          marksCode = "WAR",
          process = GradeBoundaryProcess.Reassessment,
          attempt = 1,
          rank = 1,
          grade = "R",
          Some(0),
          Some(100),
          signalStatus = NoSignal,
          result = Some(Fail),
          agreedStatus = GradeBoundaryAgreedStatus.Reassessment,
          incrementsAttempt = true
        ))

        val m1: ModuleRegistration = dbRmr4.moduleRegistration.get
        m1.membershipService = smartMock[AssessmentMembershipService]
        m1.membershipService.getUpstreamAssessmentGroups(m1, allAssessmentGroups = true, eagerLoad = true) returns Nil

        smrExporter.smoRecordExists(dbRmr4) should be (true)
        smrExporter.smrRecordSubdata(dbRmr4) should be (defined)

        val cnt: Int = smrExporter.exportModuleMarksToSits(dbRmr4, finalAssessmentAttended = false)
        session.flush()
        session.clear()
        cnt should be(1)
        val existingSmr: Option[SmrSubset] = smrExporter.smrRecordSubdata(dbRmr4)
        existingSmr.size should be(1)
        existingSmr.get.actualMark should be(Some(20))
        existingSmr.get.actualGrade should be(Some("R"))
        existingSmr.get.agreedMark should be(Some(20))
        existingSmr.get.agreedGrade should be(Some("R"))
        existingSmr.get.currentAttempt should be(Some(2))
        existingSmr.get.completedAttempt should be(Some(1))
        existingSmr.get.sasStatus should be(Some("R"))
        existingSmr.get.processStatus should be(None)
        existingSmr.get.process should be(Some("RAS"))
      }
    }
  }


  @Transactional
  @Test def smrExportWithActualFailedRecordedMark(): Unit = {
    withUser("cusxxx") {
      new ExportSMREnvironment {
        val dbRmr4: RecordedModuleRegistration = session.get(classOf[RecordedModuleRegistration].getName, rmr4.id).asInstanceOf[RecordedModuleRegistration]
        val rMark4: RecordedModuleMark = dbRmr4.addMark(uploader = currentUser.apparentUser,
          mark = Some(20),
          grade = Some("F"),
          result = Some(Fail),
          source = ComponentMarkCalculation,
          markState = UnconfirmedActual,
          comments = "Actual marks")
        smrExporter.assessmentMembershipService.gradesForMark(mr4, Some(20)) returns Nil

        val m1: ModuleRegistration = dbRmr4.moduleRegistration.get
        m1.membershipService = smartMock[AssessmentMembershipService]
        m1.membershipService.getUpstreamAssessmentGroups(m1, allAssessmentGroups = true, eagerLoad = true) returns Nil

        smrExporter.smoRecordExists(dbRmr4) should be (true)
        smrExporter.smrRecordSubdata(dbRmr4) should be (defined)

        val cnt: Int = smrExporter.exportModuleMarksToSits(dbRmr4, finalAssessmentAttended = false)
        session.flush()
        session.clear()
        cnt should be(1)
        val existingSmr: Option[SmrSubset] = smrExporter.smrRecordSubdata(dbRmr4)
        existingSmr.size should be(1)
        existingSmr.get.actualMark should be(Some(20))
        existingSmr.get.actualGrade should be(Some("F"))
        existingSmr.get.agreedMark should be(None)
        existingSmr.get.agreedGrade should be(None)
        existingSmr.get.currentAttempt should be(Some(1))
        existingSmr.get.completedAttempt should be(Some(0))
        existingSmr.get.sasStatus should be(None)
        existingSmr.get.processStatus should be(Some("C"))
        existingSmr.get.process should be(Some("SAS"))
      }
    }
  }

  @Transactional
  @Test def smrExportWithAgreedNoResitRecordedMark(): Unit = {
    withUser("cusxxx") {
      new ExportSMREnvironment {
        val dbRmr4: RecordedModuleRegistration = session.get(classOf[RecordedModuleRegistration].getName, rmr4.id).asInstanceOf[RecordedModuleRegistration]
        val rMark4: RecordedModuleMark = dbRmr4.addMark(uploader = currentUser.apparentUser,
          mark = Some(20),
          grade = Some("N"),
          result = Some(Fail),
          source = ComponentMarkCalculation,
          markState = Agreed,
          comments = "Agreed fail")
        smrExporter.assessmentMembershipService.gradesForMark(mr4, Some(20)) returns Seq(GradeBoundary (
          marksCode = "WAR",
          process = GradeBoundaryProcess.Reassessment,
          attempt = 1,
          rank = 1,
          grade = "N",
          Some(0),
          Some(100),
          signalStatus = NoSignal,
          result = Some(Fail),
          agreedStatus = GradeBoundaryAgreedStatus.Agreed,
          incrementsAttempt = false
        ))

        val m1: ModuleRegistration = dbRmr4.moduleRegistration.get
        m1.membershipService = smartMock[AssessmentMembershipService]
        m1.membershipService.getUpstreamAssessmentGroups(m1, allAssessmentGroups = true, eagerLoad = true) returns Nil

        smrExporter.smoRecordExists(dbRmr4) should be (true)
        smrExporter.smrRecordSubdata(dbRmr4) should be (defined)

        val cnt: Int = smrExporter.exportModuleMarksToSits(dbRmr4, finalAssessmentAttended = false)
        session.flush()
        session.clear()
        cnt should be(1)
        val existingSmr: Option[SmrSubset] = smrExporter.smrRecordSubdata(dbRmr4)
        existingSmr.size should be(1)
        existingSmr.get.actualMark should be(Some(20))
        existingSmr.get.actualGrade should be(Some("N"))
        existingSmr.get.agreedMark should be(Some(20))
        existingSmr.get.agreedGrade should be(Some("N"))
        existingSmr.get.currentAttempt should be(Some(1))
        existingSmr.get.completedAttempt should be(Some(1))
        existingSmr.get.sasStatus should be(Some("A"))
        existingSmr.get.processStatus should be(None)
        existingSmr.get.process should be(Some("COM"))
      }
    }
  }
}
