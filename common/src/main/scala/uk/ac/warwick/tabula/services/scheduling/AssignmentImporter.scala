package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Timestamp, Types}
import java.time.temporal.ChronoUnit
import java.time.{Instant, OffsetDateTime}

import javax.sql.DataSource
import org.joda.time._
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQuery, MappingSqlQueryWithParameters}
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.{RowCallbackHandler, SqlParameter}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.marks.AutowiringAssessmentComponentMarksServiceComponent
import uk.ac.warwick.tabula.services.scheduling.AssignmentImporter._
import uk.ac.warwick.tabula.services.timetables.AutowiringExamTimetableFetchingServiceComponent
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, Features}
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

import scala.concurrent.Await
import scala.jdk.CollectionConverters._
import scala.util.Try

trait AssignmentImporterComponent {
  def assignmentImporter: AssignmentImporter
}

trait AutowiringAssignmentImporterComponent extends AssignmentImporterComponent {
  val assignmentImporter: AssignmentImporter = Wire[AssignmentImporter]
}

trait AssignmentImporter {
  /**
   * Iterates through ALL module registration elements,
   * passing each ModuleRegistration item to the given callback for it to process.
   */
  def allMembers(yearsToImport: Seq[AcademicYear])(callback: UpstreamModuleRegistration => Unit): Unit

  def specificMembers(members: Seq[MembershipMember], yearsToImport: Seq[AcademicYear])(callback: UpstreamModuleRegistration => Unit): Unit

  def getAllAssessmentGroups(yearsToImport: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup]

  def getAllAssessmentComponents(yearsToImport: Seq[AcademicYear]): Seq[AssessmentComponent]

  def getAllGradeBoundaries: Seq[GradeBoundary]

  def getAllVariableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule]

  def getAllScheduledExams(yearsToImport: Seq[AcademicYear]): Seq[AssessmentComponentExamSchedule]

  def getScheduledExamStudents(schedule: AssessmentComponentExamSchedule): Seq[AssessmentComponentExamScheduleStudent]

  def publishedExamProfiles(yearsToImport: Seq[AcademicYear]): Seq[String]
}

@Profile(Array("dev", "test", "production"))
@Service
class AssignmentImporterImpl extends AssignmentImporter with InitializingBean
  with AutowiringSitsDataSourceComponent
  with AutowiringExamTimetableFetchingServiceComponent with AutowiringFeaturesComponent {

  var upstreamAssessmentGroupQuery: UpstreamAssessmentGroupQuery = _
  var assessmentComponentQuery: AssessmentComponentQuery = _
  var gradeBoundaryQuery: GradeBoundaryQuery = _
  var variableAssessmentWeightingRuleQuery: VariableAssessmentWeightingRuleQuery = _
  var examScheduleQuery: ExamScheduleQuery = _
  var examScheduleStudentsQuery: ExamScheduleStudentsQuery = _
  var jdbc: NamedParameterJdbcTemplate = _

  override def afterPropertiesSet(): Unit = {
    assessmentComponentQuery = new AssessmentComponentQuery(sitsDataSource)
    upstreamAssessmentGroupQuery = new UpstreamAssessmentGroupQuery(sitsDataSource)
    gradeBoundaryQuery = new GradeBoundaryQuery(sitsDataSource)
    variableAssessmentWeightingRuleQuery = new VariableAssessmentWeightingRuleQuery(sitsDataSource)
    examScheduleQuery = new ExamScheduleQuery(sitsDataSource)
    examScheduleStudentsQuery = new ExamScheduleStudentsQuery(sitsDataSource)
    jdbc = new NamedParameterJdbcTemplate(sitsDataSource)
  }

  def getAllAssessmentComponents(yearsToImport: Seq[AcademicYear]): Seq[AssessmentComponent] = {
    val currentAcademicYearCode =  if (includeSMS(yearsToImport)) {
      yearsToImportArray(yearsToImport.intersect(AcademicYear.allCurrent()))
    } else Seq("").asJava //set blank for SMS table to be ignored in the actual SQL
    val paraMap = JMap(
      "academic_year_code" -> yearsToImportArray(yearsToImport),
      "current_academic_year_code" -> currentAcademicYearCode
    )
   assessmentComponentQuery.executeByNamedParam(paraMap).asScala.toSeq
  }
  private def yearsToImportArray(yearsToImport: Seq[AcademicYear]): JList[String] = yearsToImport.map(_.toString).asJava: JList[String]

  //For academic years marked current we do import SMS data if the feature flag is on. For all other cases SMS data is ignored.
  private def includeSMS(yearsToImport: Seq[AcademicYear]): Boolean = features.includeSMSForCurrentYear && yearsToImport.intersect(AcademicYear.allCurrent()).nonEmpty

  private def seatNumberExamProfilesArray(): JList[String] =
    Await.result(examTimetableFetchingService.getExamProfiles, scala.concurrent.duration.Duration.Inf)
      .filter(_.seatNumbersPublished)
      .map(_.code)
      .asJava: JList[String]

  // This will be quite a few thousand records, but not more than
  // 20k. Shouldn't cause any memory problems, so no point complicating
  // it by trying to stream or batch the data.
  def getAllAssessmentGroups(yearsToImport: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup] = upstreamAssessmentGroupQuery.executeByNamedParam(JMap(
    "academic_year_code" -> yearsToImportArray(yearsToImport))).asScala.toSeq

  /**
   * Iterates through ALL module registration elements in SITS (that's many),
   * passing each ModuleRegistration item to the given callback for it to process.
   */
  def allMembers(yearsToImport: Seq[AcademicYear])(callback: UpstreamModuleRegistration => Unit): Unit = {
    val params: JMap[String, Object] = JMap(
      "academic_year_code" -> yearsToImportArray(yearsToImport),
      "seat_number_exam_profiles" -> seatNumberExamProfilesArray()
    )
    if (includeSMS(yearsToImport)) {
      params.putAll(JMap("current_academic_year_code" -> yearsToImportArray(yearsToImport.intersect(AcademicYear.allCurrent()))))
      jdbc.query(AssignmentImporter.GetAllAssessmentGroupMembers, params, new UpstreamModuleRegistrationRowCallbackHandler(callback))
    } else {
      jdbc.query(AssignmentImporter.GetAllAssessmentGroupMembersExcludeSMS, params, new UpstreamModuleRegistrationRowCallbackHandler(callback))
    }
  }

  def specificMembers(members: Seq[MembershipMember], yearsToImport: Seq[AcademicYear])(callback: UpstreamModuleRegistration => Unit): Unit = {
    val params: JMap[String, Object] = JMap(
      "academic_year_code" -> yearsToImport.map(_.toString).asJava,
      "seat_number_exam_profiles" -> seatNumberExamProfilesArray(),
      "universityIds" -> members.map(_.universityId).asJava
    )
    if (includeSMS(yearsToImport)) {
      params.putAll(JMap("current_academic_year_code" -> yearsToImportArray(yearsToImport.intersect(AcademicYear.allCurrent()))))
      jdbc.query(AssignmentImporter.GetModuleRegistrationsByUniversityId(members.size > 1), params, new UpstreamModuleRegistrationRowCallbackHandler(callback))
    } else {
      jdbc.query(AssignmentImporter.GetModuleRegistrationsByUniversityIdExcludingSMS(members.size > 1), params, new UpstreamModuleRegistrationRowCallbackHandler(callback))
    }
  }

  class UpstreamModuleRegistrationRowCallbackHandler(callback: UpstreamModuleRegistration => Unit) extends RowCallbackHandler {
    override def processRow(rs: ResultSet): Unit = {
      callback(UpstreamModuleRegistration(
        year = rs.getString("academic_year_code"),
        sprCode = rs.getString("spr_code"),
        seatNumber = rs.getString("seat_number"),
        occurrence = rs.getString("mav_occurrence"),
        sequence = rs.getString("sequence"),
        moduleCode = rs.getString("module_code"),
        assessmentGroup = convertAssessmentGroupFromSITS(rs.getString("assessment_group")),
        actualMark = rs.getString("actual_mark"),
        actualGrade = rs.getString("actual_grade"),
        agreedMark = rs.getString("agreed_mark"),
        agreedGrade = rs.getString("agreed_grade"),
        resitActualMark = rs.getString("resit_actual_mark"),
        resitActualGrade = rs.getString("resit_actual_grade"),
        resitAgreedMark = rs.getString("resit_agreed_mark"),
        resitAgreedGrade = rs.getString("resit_agreed_grade"),
        resitExpected = rs.getBoolean("resit_expected")
      ))
    }
  }

  /** Convert incoming null assessment groups into the NONE value */
  private def convertAssessmentGroupFromSITS(string: String) =
    if (string == null) AssessmentComponent.NoneAssessmentGroup
    else string

  override def getAllGradeBoundaries: Seq[GradeBoundary] = gradeBoundaryQuery.execute().asScala.toSeq

  override def getAllVariableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule] = variableAssessmentWeightingRuleQuery.execute().asScala.toSeq

  private[this] lazy val extraExamProfileSchedulesToImport: Seq[String] =
    Wire.property("${assignmentImporter.extraExamProfileSchedulesToImport}")
      .split(',')
      .filter(_.hasText)
      .map(_.trim())

  override def publishedExamProfiles(yearsToImport: Seq[AcademicYear]): Seq[String] = {
    // MM 20/04/2020 ignore profiles not in extraExamProfileSchedulesToImport for now, old data is a mess
    Await.result(examTimetableFetchingService.getExamProfiles, scala.concurrent.duration.Duration.Inf)
      .filter(p => yearsToImport.contains(p.academicYear) && (extraExamProfileSchedulesToImport.contains(p.code)/* || p.published || p.seatNumbersPublished*/))
      .map(_.code)
  }

  override def getAllScheduledExams(yearsToImport: Seq[AcademicYear]): Seq[AssessmentComponentExamSchedule] = {
    val examProfiles = publishedExamProfiles(yearsToImport)

    if (examProfiles.isEmpty) Seq.empty
    else examScheduleQuery.executeByNamedParam(JMap(
      "published_exam_profiles" -> (examProfiles.asJava: JList[String])
    )).asScala.toSeq
  }

  override def getScheduledExamStudents(schedule: AssessmentComponentExamSchedule): Seq[AssessmentComponentExamScheduleStudent] =
    examScheduleStudentsQuery.executeByNamedParam(JMap(
      "exam_profile_code" -> schedule.examProfileCode,
      "slot_id" -> schedule.slotId,
      "sequence" -> schedule.sequence,
      "location_sequence" -> schedule.locationSequence
    )).asScala.toSeq
}

@Profile(Array("sandbox"))
@Service
class SandboxAssignmentImporter extends AssignmentImporter
  with AutowiringAssessmentComponentMarksServiceComponent
  with AutowiringAssessmentMembershipServiceComponent {

  override def specificMembers(members: Seq[MembershipMember], yearsToImport: Seq[AcademicYear])(callback: UpstreamModuleRegistration => Unit): Unit = allMembers(yearsToImport) { umr =>
    if (members.map(_.universityId).contains(umr.universityId)) {
      callback(umr)
    }
  }

  def allMembers(yearsToImport: Seq[AcademicYear])(callback: UpstreamModuleRegistration => Unit): Unit = {
    var moduleCodesToIds = Map[String, Seq[Range]]()

    for {
      (_, d) <- SandboxData.Departments
      route <- d.routes.values.toSeq
      moduleCode <- route.moduleCodes
    } {
      val range = route.studentsStartId to route.studentsEndId

      moduleCodesToIds = moduleCodesToIds + (
        moduleCode -> (moduleCodesToIds.getOrElse(moduleCode, Seq()) :+ range)
      )
    }

    val upstreamModuleRegistrations: Seq[UpstreamModuleRegistration] =
      (for {
        (moduleCode, ranges) <- moduleCodesToIds
        assessmentType <- Seq(AssessmentType.Essay, AssessmentType.SummerExam)
        academicYear <- yearsToImport
        range <- ranges
        uniId <- range
        if moduleCode.substring(3, 4).toInt <= ((uniId % 3) + 1)
      } yield {
        val yearOfStudy = (uniId % 3) + 1
        val level = moduleCode.substring(3, 4).toInt

        if (level <= yearOfStudy && academicYear == (AcademicYear.now - (yearOfStudy - level))) uk.ac.warwick.tabula.data.Transactions.transactional() {
          val universityId = uniId.toString

          val moduleCodeFull = "%s-15".format(moduleCode.toUpperCase)
          val assessmentGroup = "A"
          val sequence = assessmentType.subtype match {
            case TabulaAssessmentSubtype.Assignment => "A01"
            case TabulaAssessmentSubtype.Exam => "E01"
          }
          val occurrence = "A"

          val template = new UpstreamAssessmentGroup
          template.academicYear = academicYear
          template.occurrence = occurrence
          template.moduleCode = moduleCodeFull
          template.sequence = sequence
          template.assessmentGroup = assessmentGroup

          val upstreamAssessmentGroup: Option[UpstreamAssessmentGroup] =
            assessmentMembershipService.getUpstreamAssessmentGroup(template)

          val upstreamAssessmentGroupMember: Option[UpstreamAssessmentGroupMember] =
            upstreamAssessmentGroup.flatMap(_.members.asScala.find(_.universityId == universityId))

          val recordedStudent: Option[RecordedAssessmentComponentStudent] =
            upstreamAssessmentGroupMember.flatMap { uagm =>
              assessmentComponentMarksService.getAllRecordedStudents(uagm.upstreamAssessmentGroup)
                .find(_.universityId == uagm.universityId)
            }

          val (mark, grade) =
            if (academicYear < AcademicYear.now()) {
              val isPassFail = moduleCode.takeRight(1) == "9" // modules with a code ending in 9 are pass/fails

              val randomModuleMark = (universityId ++ universityId ++ moduleCode.substring(3)).toCharArray.map(char =>
                Try(char.toString.toInt).toOption.getOrElse(0) * universityId.toCharArray.apply(0).toString.toInt
              ).sum % 100

              val m =
                if (isPassFail) {
                  if (randomModuleMark < 40) 0 else 100
                } else {
                  val markVariance = (universityId ++ universityId ++ moduleCode.substring(3)).toCharArray.map(char =>
                    Try(char.toString.toInt).toOption.getOrElse(0) * universityId.toCharArray.apply(0).toString.toInt
                  ).sum % 15

                  val assessmentMark = assessmentType.subtype match {
                    case TabulaAssessmentSubtype.Assignment => randomModuleMark + markVariance
                    case TabulaAssessmentSubtype.Exam => randomModuleMark - markVariance
                  }

                  Math.max(0, Math.min(100, assessmentMark))
                }

              val marksCode =
                if (isPassFail) "TABULA-PF"
                else "TABULA-UG"

              val g =
                if (isPassFail) if (m == 100) "P" else "F"
                else SandboxData.GradeBoundaries.find(gb => gb.marksCode == marksCode && gb.isValidForMark(Some(m))).map(_.grade).getOrElse("F")

              (if (isPassFail) null else m.toString, g)
            } else (null: String, null: String)

          recordedStudent.filter(_.needsWritingToSits).foreach { s =>
            s.needsWritingToSits = false
            s.lastWrittenToSits = Some(DateTime.now)
            assessmentComponentMarksService.saveOrUpdate(s)
          }

          Some(UpstreamModuleRegistration(
            year = academicYear.toString,
            sprCode = "%d/1".format(uniId),
            seatNumber = assessmentType.subtype match {
              case TabulaAssessmentSubtype.Exam => ((uniId % 300) + 1).toString
              case _ => null
            },
            occurrence = occurrence,
            sequence = sequence,
            moduleCode = moduleCodeFull,
            assessmentGroup = assessmentGroup,
            actualMark = recordedStudent.flatMap(_.latestMark).map(_.toString).getOrElse(mark),
            actualGrade = recordedStudent.flatMap(_.latestGrade).getOrElse(grade),
            agreedMark = if (academicYear < AcademicYear.now()) mark else null,
            agreedGrade = if (academicYear < AcademicYear.now()) grade else null,
            resitActualMark = null,
            resitActualGrade = null,
            resitAgreedMark = null,
            resitAgreedGrade = null,
            resitExpected = false
          ))
        } else None
      }).flatten.toSeq

    upstreamModuleRegistrations.sortBy(umr => (umr.year, umr.occurrence, umr.moduleCode, umr.assessmentGroup)).foreach(callback)
  }

  def getAllAssessmentGroups(yearsToImport: Seq[AcademicYear]): Seq[UpstreamAssessmentGroup] =
    (for {
      (_, d) <- SandboxData.Departments.toSeq
      route <- d.routes.values.toSeq
      moduleCode <- route.moduleCodes
      assessmentType <- Seq(AssessmentType.Essay, AssessmentType.SummerExam)
      academicYear <- yearsToImport
    } yield (moduleCode, assessmentType, academicYear)).zipWithIndex.map { case ((moduleCode, assessmentType, academicYear), index) =>
      val ag = new UpstreamAssessmentGroup()
      ag.moduleCode = "%s-15".format(moduleCode.toUpperCase)
      ag.academicYear = academicYear
      ag.assessmentGroup = "A"
      ag.occurrence = "A"
      ag.sequence = assessmentType match {
        case AssessmentType.Essay => "A01"
        case AssessmentType.SummerExam => "E01"
      }
      ag.deadline = Some(assessmentType match {
        case AssessmentType.Essay =>
          index % 20 match {
            case i if i > 10 =>
              academicYear.termOrVacation(PeriodType.springTerm)
                .firstDay.withDayOfWeek((index % 5) + 1)
                .plusWeeks(index % 10)

            case i =>
              academicYear.termOrVacation(PeriodType.autumnTerm)
                .firstDay.withDayOfWeek((index % 5) + 1)
                .plusWeeks(i)
          }

        case AssessmentType.SummerExam =>
          new LocalDate(academicYear.endYear, DateTimeConstants.APRIL, 27)
            .plusDays(index / 10)
      })

      ag
    }

  def getAllAssessmentComponents(yearsToImport: Seq[AcademicYear]): Seq[AssessmentComponent] =
    for {
      (_, d) <- SandboxData.Departments.toSeq
      route <- d.routes.values.toSeq
      moduleCode <- route.moduleCodes
      module <- d.modules.get(moduleCode).toSeq
      assessmentType <- Seq(AssessmentType.Essay, AssessmentType.SummerExam)
    } yield assessmentType match {
      case AssessmentType.Essay =>
        val a = new AssessmentComponent
        a.moduleCode = "%s-15".format(moduleCode.toUpperCase)
        a.sequence = "A01"
        a.name = "Report (2,000 words)"
        a.assessmentGroup = "A"
        a.assessmentType = AssessmentType.Essay
        a.inUse = true
        a.rawWeighting = 30

        val isPassFail = moduleCode.takeRight(1) == "9" // modules with a code ending in 9 are pass/fails
        a.marksCode =
          if (isPassFail) "TABULA-PF"
          else route.degreeType match {
            case DegreeType.Postgraduate => "TABULA-PG"
            case _ => "TABULA-UG"
          }

        a.examPaperCode = None
        a.examPaperTitle = None
        a.examPaperSection = None
        a.examPaperDuration = None
        a.examPaperReadingTime = None
        a.examPaperType = None
        a

      case AssessmentType.SummerExam =>
        val e = new AssessmentComponent
        e.moduleCode = "%s-15".format(moduleCode.toUpperCase)
        e.sequence = "E01"
        e.name = "2 hour examination (Summer)"
        e.assessmentGroup = "A"
        e.assessmentType = AssessmentType.SummerExam
        e.inUse = true
        e.rawWeighting = 70

        val isPassFail = moduleCode.takeRight(1) == "9" // modules with a code ending in 9 are pass/fails
        e.marksCode =
          if (isPassFail) "TABULA-PF"
          else route.degreeType match {
            case DegreeType.Postgraduate => "TABULA-PG"
            case _ => "TABULA-UG"
          }

        e.examPaperCode = Some(s"${moduleCode.toUpperCase}0")
        e.examPaperTitle = Some(module.name)
        e.examPaperSection = Some("n/a")
        e.examPaperDuration = Some(Duration.standardMinutes(120))
        e.examPaperReadingTime = None
        e.examPaperType = Some(ExaminationType.Standard)
        e
    }

  override def getAllGradeBoundaries: Seq[GradeBoundary] = SandboxData.GradeBoundaries

  override def getAllVariableAssessmentWeightingRules: Seq[VariableAssessmentWeightingRule] = Seq.empty

  override def getAllScheduledExams(yearsToImport: Seq[AcademicYear]): Seq[AssessmentComponentExamSchedule] =
    (for {
      (_, d) <- SandboxData.Departments.toSeq
      route <- d.routes.values.toSeq
      moduleCode <- route.moduleCodes
      year <- yearsToImport
    } yield (moduleCode, year)).distinct.zipWithIndex.map { case ((moduleCode, year), index) =>
      val a = new AssessmentComponentExamSchedule
      a.moduleCode = "%s-15".format(moduleCode.toUpperCase)
      a.assessmentComponentSequence = "E01"
      a.examProfileCode = s"EXSUM${year.endYear % 100}"
      a.slotId = f"${(index / 5) + 1}%03d"
      a.sequence = f"${(index % 5) + 1}%03d" // Five exams per slot
      a.locationSequence = "001"
      a.academicYear = year
      a.startTime =
        new LocalDate(year.endYear, DateTimeConstants.APRIL, 27)
          .plusDays(index / 10)
          .toDateTime(if (index % 10 < 5) new LocalTime(9, 0) else new LocalTime(14, 0))
      a.examPaperCode = s"${moduleCode.toUpperCase}0"
      a.examPaperSection = Some("n/a")
      a.location = Some(NamedLocation("Panorama Room"))
      a
    }

  override def getScheduledExamStudents(schedule: AssessmentComponentExamSchedule): Seq[AssessmentComponentExamScheduleStudent] = {
    var students: Seq[AssessmentComponentExamScheduleStudent] = Seq()

    allMembers(Seq(schedule.academicYear)) { modReg =>
      if (modReg.moduleCode == schedule.moduleCode && modReg.sequence == schedule.assessmentComponentSequence) {
        val student = new AssessmentComponentExamScheduleStudent
        student.seatNumber = Some(students.size + 1)
        student.universityId = modReg.universityId
        student.sprCode = modReg.sprCode
        student.occurrence = modReg.occurrence

        students = students :+ student
      }
    }

    students
  }

  override def publishedExamProfiles(yearsToImport: Seq[AcademicYear]): Seq[String] =
    yearsToImport.map(year => s"EXSUM${year.endYear % 100}")
}


object AssignmentImporter {
  var sitsSchema: String = Wire.property("${schema.sits}")
  var features: Features = Wire[Features]
  var sqlStringCastFunction: String = "to_char"
  var dialectRegexpLike = "regexp_like"

  // Because we have a mismatch between nvarchar2 and chars in the text, we need to cast some results to chars in Oracle (for SITS), but not in HSQL (with the embedded database)
  def castToString(orig: String): String =
    if (sqlStringCastFunction.hasText) s"$sqlStringCastFunction($orig)"
    else orig

  /**
   * Get AssessmentComponents, and also some fake ones for linking to
   * the group of students with no selected assessment group.
   *
   * The actual assessment components come from CAM_MAB ("Module Assessment Body") which contains the
   * assessment components which make up modules.
   * This is unioned with module registrations (in SMS and SMO) where assessment group (SMS_AGRP and SMO_AGRP) is not
   * specified.
   *
   * SMS holds unconfirmed module registrations and is included to catch module registrations not approved yet.
   * SMO holds confirmed module registrations and is included to catch module registrations in departments which
   * upload module registrations after confirmation.
   *
   * Remember this could be for previous years so don't make decisions based on whether the module is _currently_ in use.
   */
  def GetAssessmentsQuery =
    s"""
    select distinct
      sms.mod_code as module_code,
      '${AssessmentComponent.NoneAssessmentGroup}' as seq,
      'Students not registered for assessment' as name,
      '${AssessmentComponent.NoneAssessmentGroup}' as assessment_group,
      'X' as assessment_code,
      'Y' as in_use,
      null as marks_code,
      0 as weight,
      null as exam_paper_code,
      null as exam_paper_title,
      null as exam_paper_section,
      null as exam_paper_duration,
      null as exam_paper_reading_time,
      null as exam_paper_type
      from $sitsSchema.cam_sms sms
        join $sitsSchema.cam_ssn ssn -- SSN table holds module registration status
          on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON' -- mrgs = "Module Registration Status"
      where
        sms.sms_agrp is null and -- assessment group, ie group of assessment components which together represent an assessment choice
        sms.ayr_code in (:current_academic_year_code)
  union all
    select distinct
      smo.mod_code as module_code,
      '${AssessmentComponent.NoneAssessmentGroup}' as seq,
      'Students not registered for assessment' as name,
      '${AssessmentComponent.NoneAssessmentGroup}' as assessment_group,
      'X' as assessment_code,
      'Y' as in_use,
      null as marks_code,
      0 as weight,
      null as exam_paper_code,
      null as exam_paper_title,
      null as exam_paper_section,
      null as exam_paper_duration,
      null as exam_paper_reading_time,
      null as exam_paper_type
      from $sitsSchema.cam_smo smo
        left outer join $sitsSchema.cam_ssn ssn
          on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code
      where -- RTSC is used by WMG to indicate attendance status.  X = cancelled, Z = module cancelled
        (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) and
        ssn.ssn_sprc is null and -- there is no module registration status row, so this SMO has been uploaded rather than created in SITS
        smo.smo_agrp is null and -- assessment group, ie group of assessment components which together represent an assessment choice
        smo.ayr_code in (:academic_year_code)
  union all
    select
      mab.map_code as module_code,
      ${castToString("mab.mab_seq")} as seq,
      ${castToString("mab.mab_name")} as name,
      ${castToString("mab.mab_agrp")} as assessment_group,
      ${castToString("mab.ast_code")} as assessment_code,
      ${castToString("mab.mab_udf1")} as in_use,
      ${castToString("mab.mks_code")} as marks_code,
      mab.mab_perc as weight,
      ${castToString("mab.mab_apac")} as exam_paper_code,
      ${castToString("apa.apa_name")} as exam_paper_title,
      case when (mab.mab_advc = 'X') then 'n/a' else ${castToString("mab.mab_advc")} end as exam_paper_section,
      coalesce(mab.mab_hohm, adv.adv_dura) as exam_paper_duration,
      adv.adv_rdtm as exam_paper_reading_time,
      ${castToString("apa.apa_aptc")} as exam_paper_type
      from $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components
        join $sitsSchema.cam_mav mav -- Module Availability which indicates which modules are avaiable in the year
          on mab.map_code = mav.mod_code and
             mav.psl_code = 'Y' and -- "Period Slot" code - Y indicates year
             mav.ayr_code in (:academic_year_code)
        join $sitsSchema.ins_mod mod
          on mav.mod_code = mod.mod_code
        left outer join $sitsSchema.cam_apa apa -- paper
          on mab.mab_apac = apa.apa_code
        left outer join $sitsSchema.cam_adv adv -- paper division (section)
          on mab.mab_apac = adv.adv_apac and mab.mab_advc = adv.adv_code
      where mab.mab_agrp is not null"""

  def GetAllAssessmentGroups =
    s"""
    select distinct
      mav.ayr_code as academic_year_code,
      mav.mod_code as module_code,
      '${AssessmentComponent.NoneAssessmentGroup}' as mav_occurrence,
      '${AssessmentComponent.NoneAssessmentGroup}' as assessment_group,
      '${AssessmentComponent.NoneAssessmentGroup}' as seq,
      null as deadline
      from $sitsSchema.cam_mab mab
        join $sitsSchema.cam_mav mav
          on mab.map_code = mav.mod_code
        join $sitsSchema.ins_mod mod
          on mav.mod_code = mod.mod_code
      where mav.psl_code = 'Y' and -- period slot code of Y (year)
            mav.ayr_code in (:academic_year_code)
  union all
    select distinct
      mav.ayr_code as academic_year_code,
      mav.mod_code as module_code,
      ${castToString("mav.mav_occur")} as mav_occurrence, -- module occurrence (representing eg day or evening - usually 'A')
      ${castToString("mab.mab_agrp")} as assessment_group, -- group of assessment components forming one assessment choice
      ${castToString("mab.mab_seq")} as seq, -- individual assessments (e.g each exam or coursework component)
      mad.mad_ddate as deadline
      from $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components
        join $sitsSchema.cam_mav mav -- Module Availability which indicates which modules are available in the year
          on mab.map_code = mav.mod_code
        join $sitsSchema.ins_mod mod
          on mav.mod_code = mod.mod_code
        left outer join $sitsSchema.cam_mad mad -- Module Assessment... Deadline? one-to-one to mav/mab?
          on mav.mod_code = mad.mod_code and
             mav.mav_occur = mad.mav_occur and
             mav.ayr_code = mad.ayr_code and
             mav.psl_code = mad.psl_code and
             mab.map_code = mad.map_code and
             mab.mab_seq = mad.mab_seq
      where mav.psl_code = 'Y' and
            mab.mab_agrp is not null and
            mav.ayr_code in (:academic_year_code)"""

  // for students who register for modules through SITS,this gets their assessments before their choices are confirmed
  // We only refer to unconfirmed choices for current academic year based on the feature flag.
  def GetUnconfirmedModuleRegistrations =
    s"""
    select
      sms.ayr_code as academic_year_code,
      spr.spr_code as spr_code,
      wss.wss_seat as seat_number,
      sms.sms_occl as mav_occurrence, -- module occurrence (representing eg day or evening - usually 'A')
      sms.mod_code as module_code,
      sms.sms_agrp as assessment_group,
      mab.mab_seq as sequence,
      sas.sas_actm as actual_mark,
      sas.sas_actg as actual_grade,
      sas.sas_agrm as agreed_mark,
      sas.sas_agrg as agreed_grade,
      sra.sra_actm as resit_actual_mark,
      sra.sra_actg as resit_actual_grade,
      sra.sra_agrm as resit_agreed_mark,
      sra.sra_agrg as resit_agreed_grade,
      case when (sas.sas_sass = 'R') then 1 else 0 end as resit_expected
        from $sitsSchema.srs_scj scj -- Student Course Join  - gives us most significant course
          join $sitsSchema.ins_spr spr -- Student Programme Route - gives us SPR code
            on scj.scj_sprc = spr.spr_code and
              (spr.sts_code is null or spr.sts_code != 'D') -- no deceased students

          join $sitsSchema.cam_sms sms -- Student Module Selection table, storing unconfirmed module registrations
            on sms.spr_code = scj.scj_sprc

          join $sitsSchema.cam_ssn ssn -- SSN holds module registration status
            on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON' -- module choices confirmed

          left join $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components (needed for the sequences)
            on mab.map_code = sms.mod_code and mab.mab_agrp = sms.sms_agrp

          left join $sitsSchema.cam_wss wss -- WSS is "Slot Student"
            on wss.wss_sprc = spr.spr_code and wss.wss_ayrc = sms.ayr_code and wss.wss_modc = sms.mod_code
              and wss.wss_mabs = mab.mab_seq and wss.wss_wspc in (:seat_number_exam_profiles)

          left join $sitsSchema.cam_sas sas -- Where component marks go
            on sas.spr_code = sms.spr_code and sas.ayr_code = sms.ayr_code and sas.mod_code = sms.mod_code
              and sas.mav_occur = sms.sms_occl and sas.mab_seq = mab.mab_seq

          left join $sitsSchema.cam_sra sra -- Where resit marks go
            on sra.spr_code = sms.spr_code and sra.ayr_code = sms.ayr_code and sra.mod_code = sms.mod_code
              and sra.mav_occur = sms.sms_occl and sra.sra_seq = mab.mab_seq

      where
        sms.ayr_code in (:current_academic_year_code)"""

  // this gets a student's assessments from the SMO table, which stores confirmed module choices
  def GetConfirmedModuleRegistrations =
    s"""
    select
      smo.ayr_code as academic_year_code,
      spr.spr_code as spr_code,
      wss.wss_seat as seat_number,
      smo.mav_occur as mav_occurrence, -- module occurrence (representing eg day or evening - usually 'A')
      smo.mod_code as module_code,
      smo.smo_agrp as assessment_group,
      mab.mab_seq as sequence,
      sas.sas_actm as actual_mark,
      sas.sas_actg as actual_grade,
      sas.sas_agrm as agreed_mark,
      sas.sas_agrg as agreed_grade,
      sra.sra_actm as resit_actual_mark,
      sra.sra_actg as resit_actual_grade,
      sra.sra_agrm as resit_agreed_mark,
      sra.sra_agrg as resit_agreed_grade,
      case when (sas.sas_sass = 'R') then 1 else 0 end as resit_expected
        from $sitsSchema.srs_scj scj
          join $sitsSchema.ins_spr spr
            on scj.scj_sprc = spr.spr_code and
              (spr.sts_code is null or spr.sts_code != 'D') -- no deceased students

          join $sitsSchema.cam_smo smo
            on smo.spr_code = spr.spr_code and
              (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- no WMG cancelled

          join $sitsSchema.cam_ssn ssn
            on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code and ssn.ssn_mrgs = 'CON' -- confirmed module choices

          left join $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components (needed for the sequences)
            on mab.map_code = smo.mod_code and mab.mab_agrp = smo.smo_agrp

          left join $sitsSchema.cam_wss wss -- WSS is "Slot Student"
            on wss.wss_sprc = spr.spr_code and wss.wss_ayrc = smo.ayr_code and wss.wss_modc = smo.mod_code
              and wss.wss_mabs = mab.mab_seq and wss.wss_wspc in (:seat_number_exam_profiles)

          left join $sitsSchema.cam_sas sas -- Where component marks go
            on sas.spr_code = smo.spr_code and sas.ayr_code = smo.ayr_code and sas.mod_code = smo.mod_code
              and sas.mav_occur = smo.mav_occur and sas.mab_seq = mab.mab_seq

           left join $sitsSchema.cam_sra sra -- Where resit marks go
            on sra.spr_code = smo.spr_code and sra.ayr_code = smo.ayr_code and sra.mod_code = smo.mod_code
              and sra.mav_occur = smo.mav_occur and sra.sra_seq = mab.mab_seq

      where
        smo.ayr_code in (:academic_year_code)"""

  def GetAutoUploadedConfirmedModuleRegistrations =
    s"""
    select
      smo.ayr_code as academic_year_code,
      spr.spr_code as spr_code,
      wss.wss_seat as seat_number,
      smo.mav_occur as mav_occurrence,
      smo.mod_code as module_code,
      smo.smo_agrp as assessment_group,
      mab.mab_seq as sequence,
      sas.sas_actm as actual_mark,
      sas.sas_actg as actual_grade,
      sas.sas_agrm as agreed_mark,
      sas.sas_agrg as agreed_grade,
      sra.sra_actm as resit_actual_mark,
      sra.sra_actg as resit_actual_grade,
      sra.sra_agrm as resit_agreed_mark,
      sra.sra_agrg as resit_agreed_grade,
      case when (sas.sas_sass = 'R') then 1 else 0 end as resit_expected
        from $sitsSchema.srs_scj scj
          join $sitsSchema.ins_spr spr
            on scj.scj_sprc = spr.spr_code and
              (spr.sts_code is null or spr.sts_code != 'D') -- no deceased students

          join $sitsSchema.cam_smo smo
            on smo.spr_code = spr.spr_code and
              (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- no WMG cancelled

          left outer join $sitsSchema.cam_ssn ssn
            on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code

          left join $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components (needed for the sequences)
            on mab.map_code = smo.mod_code and mab.mab_agrp = smo.smo_agrp

          left join $sitsSchema.cam_wss wss -- WSS is "Slot Student"
            on wss.wss_sprc = spr.spr_code and wss.wss_ayrc = smo.ayr_code and wss.wss_modc = smo.mod_code
              and wss.wss_mabs = mab.mab_seq and wss.wss_wspc in (:seat_number_exam_profiles)

          left join $sitsSchema.cam_sas sas -- Where component marks go
            on sas.spr_code = smo.spr_code and sas.ayr_code = smo.ayr_code and sas.mod_code = smo.mod_code
              and sas.mav_occur = smo.mav_occur and sas.mab_seq = mab.mab_seq

          left join $sitsSchema.cam_sra sra -- Where resit marks go
            on sra.spr_code = smo.spr_code and sra.ayr_code = smo.ayr_code and sra.mod_code = smo.mod_code
              and sra.mav_occur = smo.mav_occur and sra.sra_seq = mab.mab_seq

      where
        smo.ayr_code in (:academic_year_code) and
        ssn.ssn_sprc is null -- no matching SSN"""

  def GetAllAssessmentGroupMembers =
    s"""
      $GetUnconfirmedModuleRegistrations
        union all
      $GetConfirmedModuleRegistrations
        union all
      $GetAutoUploadedConfirmedModuleRegistrations
    order by academic_year_code, module_code, assessment_group, mav_occurrence, sequence, spr_code"""

  def GetAllAssessmentGroupMembersExcludeSMS =
    s"""
      $GetConfirmedModuleRegistrations
        union all
      $GetAutoUploadedConfirmedModuleRegistrations
    order by academic_year_code, module_code, assessment_group, mav_occurrence, sequence, spr_code"""

  def GetModuleRegistrationsByUniversityIdSprClause(multipleUniIds: Boolean): String = {
    if (multipleUniIds) {
      s" and SUBSTR(spr.spr_code, 0, 7) in (:universityIds)"
    } else {
      s" and spr.spr_code like :universityIds || '%'"
    }
  }

  /** Looks like we are always using this for single uni Id but leaving the prior condition in case something is still using it and we don't break that **/
  def GetModuleRegistrationsByUniversityId(multipleUniIds: Boolean): String = {
    val sprClause = GetModuleRegistrationsByUniversityIdSprClause(multipleUniIds)
    s"""
      $GetUnconfirmedModuleRegistrations
        $sprClause
        union all
      $GetConfirmedModuleRegistrations
        $sprClause
        union all
      $GetAutoUploadedConfirmedModuleRegistrations
        $sprClause
    order by academic_year_code, module_code, assessment_group, mav_occurrence, sequence, spr_code"""
  }

  /** Looks like we are always using this for single uni Id but leaving the prior condition in case something is still using it and we don't break that **/
  def GetModuleRegistrationsByUniversityIdExcludingSMS(multipleUniIds: Boolean): String = {
    val sprClause = GetModuleRegistrationsByUniversityIdSprClause(multipleUniIds)
    s"""
      $GetConfirmedModuleRegistrations
        $sprClause
        union all
      $GetAutoUploadedConfirmedModuleRegistrations
        $sprClause
    order by academic_year_code, module_code, assessment_group, mav_occurrence, sequence, spr_code"""
  }

  def GetAllGradeBoundaries: String =
    s"""
    select
      mkc.mks_code as marks_code,
      mkc.mkc_grade as grade,
      mkc.mkc_minm as minimum_mark,
      mkc.mkc_maxm as maximum_mark,
      mkc.mkc_sigs as signal_status
    from $sitsSchema.cam_mkc mkc
    where mkc_proc = 'SAS'
  """

  def GetAllVariableAssessmentWeightingRules: String =
    s"""
       |select
       |    vaw.vaw_mapc as module_code,
       |    vaw.vaw_seqn as rule_sequence,
       |    vaw.vaw_agrp as assessment_group,
       |    vaw.vaw_awgt as weighting,
       |    vaw.vaw_atcc as assessment_type
       |from $sitsSchema.cam_vaw vaw
       |where vaw.vaw_atcc is not null
       |""".stripMargin

  def GetExamSchedule: String =
    s"""
       |select
       |    wsl.wsl_wspc, -- Exam profile code,
       |    wsl.wsl_seqn, -- WASP Slot number
       |    wsl.wsl_date, -- Date of the exam
       |    wsl.wsl_begt, -- Start time of the exam (OE will likely ignore this)
       |    wsm.wsm_seqn, -- WASP Assessment sequence (exam within slot)
       |    wsm.wsm_rseq, -- Assessment room sequence (multiple rooms for same exam)
       |    wsm.wsm_ayrc, -- Academic year code (resits???)
       |    wsm.wsm_modc, -- Module code
       |    wsm.wsm_mapc, -- MAP code
       |    wsm.wsm_mabs, -- MAB sequence
       |    wsm.wsm_prcc, -- Personnel code (invigilator? module leader?)
       |    wsm.wsm_apac, -- Paper code
       |    wsm.wsm_advc, -- Paper division (section) code
       |    wsm.wsm_romc, -- Room code
       |    rom.rom_name  -- Room name
       |from $sitsSchema.cam_wsl wsl -- WASP Exam Scheduling Slot
       |    join $sitsSchema.cam_wsm wsm -- WASP Module Assessment
       |        on wsl.wsl_wspc = wsm.wsm_wspc and wsl.wsl_seqn = wsm.wsm_wsls
       |    left outer join $sitsSchema.ins_rom rom -- Room
       |        on wsm.wsm_romc = rom.rom_code
       |where wsl.wsl_wspc in (:published_exam_profiles)
       |""".stripMargin

  def GetExamScheduleStudents: String =
    s"""
       |select distinct
       |    wss.wss_seat, -- Seat number
       |    wss.wss_stuc, -- University ID
       |    wss.wss_sprc, -- SPR code
       |    wss.wss_mavo  -- MAV occurrence
       |from $sitsSchema.cam_wsl wsl -- WASP Exam Scheduling Slot
       |    join $sitsSchema.cam_wsm wsm -- WASP Module Assessment
       |        on wsl.wsl_wspc = wsm.wsm_wspc and wsl.wsl_seqn = wsm.wsm_wsls
       |    join $sitsSchema.cam_wss wss
       |        on wsl.wsl_wspc = wss.wss_wspc and wsl.wsl_seqn = wss.wss_wsls and wsm.wsm_seqn = wss.wss_wsms
       |          and (
       |            -- Student has been scheduled, or
       |            wsm.wsm_rseq = wss.wss_rseq or (
       |
       |              -- Student is not scheduled and
       |              wss.wss_rseq is null and (
       |                -- Student has been allocated a room and it's the earliest matching WSM for this room, or
       |                (
       |                  wsm.wsm_romc = wss.wss_romc and
       |                  :location_sequence = (
       |                    select min(wsm2.wsm_rseq)
       |                      from $sitsSchema.cam_wsm wsm2
       |                      where wsm2.wsm_wspc = wsm.wsm_wspc
       |                        and wsm2.wsm_wsls = wsm_wsls
       |                        and wsm2.wsm_seqn = wsm.wsm_seqn
       |                        and wsm2.wsm_romc = wsm.wsm_romc
       |                  )
       |                ) or
       |
       |                -- Student hasn't been allocated a room and it's the earliest matching WSM
       |                (
       |                  wss.wss_romc is null and
       |                  :location_sequence = (
       |                    select min(wsm2.wsm_rseq)
       |                      from $sitsSchema.cam_wsm wsm2
       |                      where wsm2.wsm_wspc = wsm.wsm_wspc
       |                        and wsm2.wsm_wsls = wsm_wsls
       |                        and wsm2.wsm_seqn = wsm.wsm_seqn
       |                  )
       |                )
       |              )
       |            )
       |          ) -- TAB-8287
       |where wsl.wsl_wspc = :exam_profile_code
       |  and wsl.wsl_seqn = :slot_id
       |  and wsm.wsm_seqn = :sequence
       |  and wsm.wsm_rseq = :location_sequence
       |order by wss.wss_seat, wss.wss_stuc
       |""".stripMargin

  class AssessmentComponentQuery(ds: DataSource) extends MappingSqlQuery[AssessmentComponent](ds, GetAssessmentsQuery) {
    declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
    declareParameter(new SqlParameter("current_academic_year_code", Types.VARCHAR))

    compile()

    private val referenceDate: Instant = OffsetDateTime.parse("1900-01-01T00:00Z").toInstant

    private def dateToDuration(ts: Timestamp): Duration =
      Duration.standardMinutes(referenceDate.until(ts.toInstant, ChronoUnit.MINUTES))

    override def mapRow(rs: ResultSet, rowNumber: Int): AssessmentComponent = {
      val a = new AssessmentComponent
      a.moduleCode = rs.getString("module_code")
      a.sequence = rs.getString("seq")
      a.name = rs.getString("name")
      a.assessmentGroup = rs.getString("assessment_group")
      a.assessmentType = AssessmentType.factory(rs.getString("assessment_code"))
      a.inUse = rs.getString("in_use") match {
        case "Y" | "y" => true
        case _ => false
      }
      a.marksCode = rs.getString("marks_code")
      a.rawWeighting = rs.getInt("weight")
      a.examPaperCode = Option(rs.getString("exam_paper_code"))
      a.examPaperTitle = Option(rs.getString("exam_paper_title"))
      a.examPaperSection = Option(rs.getString("exam_paper_section"))
      a.examPaperDuration = Option(rs.getTimestamp("exam_paper_duration")).map(dateToDuration)
      a.examPaperReadingTime = if (rs.getTimestamp("exam_paper_reading_time") != null) Some(Duration.standardMinutes(15)) else None
      a.examPaperType = Option(rs.getString("exam_paper_type")).map(ExaminationType.withName)
      a
    }
  }

  class UpstreamAssessmentGroupQuery(ds: DataSource) extends MappingSqlQueryWithParameters[UpstreamAssessmentGroup](ds, GetAllAssessmentGroups) {
    declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
    this.compile()

    override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): UpstreamAssessmentGroup =
      mapRowToAssessmentGroup(rs)
  }

  def mapRowToAssessmentGroup(rs: ResultSet): UpstreamAssessmentGroup = {
    val ag = new UpstreamAssessmentGroup()
    ag.moduleCode = rs.getString("module_code")
    ag.academicYear = AcademicYear.parse(rs.getString("academic_year_code"))
    ag.assessmentGroup = rs.getString("assessment_group")
    ag.occurrence = rs.getString("mav_occurrence")
    ag.sequence = rs.getString("seq")
    ag.deadline = Option(rs.getDate("deadline")).map(_.toLocalDate.asJoda)
    ag
  }

  class GradeBoundaryQuery(ds: DataSource) extends MappingSqlQuery[GradeBoundary](ds, GetAllGradeBoundaries) {
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): GradeBoundary = {
      def getNullableInt(column: String): Option[Int] = {
        val intValue = rs.getInt(column)
        if (rs.wasNull()) None else Some(intValue)
      }

      GradeBoundary(
        rs.getString("marks_code"),
        rs.getString("grade"),
        getNullableInt("minimum_mark"),
        getNullableInt("maximum_mark"),
        rs.getString("signal_status")
      )
    }
  }

  class VariableAssessmentWeightingRuleQuery(ds: DataSource) extends MappingSqlQuery[VariableAssessmentWeightingRule](ds, GetAllVariableAssessmentWeightingRules) {
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): VariableAssessmentWeightingRule = {
      val rule = new VariableAssessmentWeightingRule
      rule.moduleCode = rs.getString("module_code")
      rule.ruleSequence = rs.getString("rule_sequence")
      rule.assessmentGroup = rs.getString("assessment_group")
      rule.weighting = rs.getInt("weighting")
      rule.assessmentType = AssessmentType.factory(rs.getString("assessment_type"))
      rule
    }
  }

  class ExamScheduleQuery(ds: DataSource) extends MappingSqlQuery[AssessmentComponentExamSchedule](ds, GetExamSchedule) {
    declareParameter(new SqlParameter("published_exam_profiles", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): AssessmentComponentExamSchedule = {
      val a = new AssessmentComponentExamSchedule
      a.moduleCode = rs.getString("wsm_mapc") // Use MAP code to match AssessmentComponent
      a.assessmentComponentSequence = rs.getString("wsm_mabs")
      a.examProfileCode = rs.getString("wsl_wspc")
      a.slotId = rs.getString("wsl_seqn")
      a.sequence = rs.getString("wsm_seqn")
      a.locationSequence = rs.getString("wsm_rseq")
      a.academicYear = AcademicYear.parse(rs.getString("wsm_ayrc"))
      a.startTime =
        rs.getDate("wsl_date").toLocalDate
          .atTime(rs.getTimestamp("wsl_begt").toLocalDateTime.toLocalTime)
          .asJoda.toDateTime
      a.examPaperCode = rs.getString("wsm_apac")
      a.examPaperSection = rs.getString("wsm_advc").maybeText
      a.location =
        rs.getString("rom_name").maybeText
          .orElse(rs.getString("wsm_romc").maybeText)
          .map(NamedLocation)
      a
    }
  }

  class ExamScheduleStudentsQuery(ds: DataSource) extends MappingSqlQuery[AssessmentComponentExamScheduleStudent](ds, GetExamScheduleStudents) {
    declareParameter(new SqlParameter("exam_profile_code", Types.VARCHAR))
    declareParameter(new SqlParameter("slot_id", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("location_sequence", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): AssessmentComponentExamScheduleStudent = {
      val s = new AssessmentComponentExamScheduleStudent
      s.seatNumber = ImportMemberHelpers.getInteger(rs, "wss_seat")
      s.universityId = rs.getString("wss_stuc")
      s.sprCode = rs.getString("wss_sprc")
      s.occurrence = rs.getString("wss_mavo")
      s
    }
  }

}
