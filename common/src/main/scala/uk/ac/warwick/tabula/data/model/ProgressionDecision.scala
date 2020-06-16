package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * A progression decision for a student (linked by SPR code).
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class ProgressionDecision extends GeneratedId with ToString {

  @Column(name = "spr_code", nullable = false)
  var sprCode: String = _

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "StudentCourseDetails_ProgressionDecision",
    joinColumns = Array(new JoinColumn(name = "progression_decision_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "scjcode", insertable = false, updatable = false))
  )
  @JoinColumn(name = "scjcode", insertable = false, updatable = false)
  @BatchSize(size = 200)
  var _allStudentCourseDetails: JSet[StudentCourseDetails] = JHashSet()

  def studentCourseDetails: Option[StudentCourseDetails] =
    _allStudentCourseDetails.asScala.find(_.mostSignificant)
      .orElse(_allStudentCourseDetails.asScala.maxByOption(_.scjCode))

  @Column(nullable = false)
  var sequence: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ProgressionDecisionOutcomeUserType")
  @Column(nullable = false)
  var outcome: ProgressionDecisionOutcome = _

  @Column(name = "notes")
  private var _notes: String = _
  def notes: Option[String] = _notes.maybeText
  def notes_=(notes: Option[String]): Unit = _notes = notes.orNull

  @Column(name = "resit_period", nullable = false)
  var resitPeriod: Boolean = _

  def isVisibleToStudent: Boolean =
    if (academicYear < AcademicYear.now()) {
      // The past is the past
      true
    } else if (studentCourseDetails.exists(scd => scd.course.code.startsWith("D") && scd.latestStudentCourseYearDetails.yearOfStudy == 1)) {
      // First year degree apprenticeship
      !new LocalDate(2020, DateTimeConstants.JULY, 29).isAfter(LocalDate.now())
    } else if (studentCourseDetails.exists(scd => scd.courseType.contains(CourseType.UG) && scd.latestStudentCourseYearDetails.yearOfStudy == 1)) {
      // First year UG
      !new LocalDate(2020, DateTimeConstants.JULY, 9).isAfter(LocalDate.now())
    } else if (studentCourseDetails.exists(scd => scd.courseType.contains(CourseType.UG) && scd.latestStudentCourseYearDetails.isFinalYear)) {
      // Finalist UG
      !new LocalDate(2020, DateTimeConstants.JULY, 22).isAfter(LocalDate.now())
    } else if (studentCourseDetails.exists(scd => scd.courseType.contains(CourseType.UG))) {
      // Intermediate UG
      !new LocalDate(2020, DateTimeConstants.JULY, 30).isAfter(LocalDate.now())
    } else {
      // who dis
      false
    }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "sequence" -> sequence,
    "academicYear" -> academicYear,
    "outcome" -> outcome,
    "notes" -> notes,
    "resitPeriod" -> resitPeriod
  )
}

sealed abstract class ProgressionDecisionOutcome(val pitCodes: Set[String]) extends EnumEntry
object ProgressionDecisionOutcome extends Enum[ProgressionDecisionOutcome] {
  // Common suffixes:
  // -S decision in September
  // -D student is a debtor
  // We don't care about the distinction in Tabula

  case object Held extends ProgressionDecisionOutcome(Set("H"))
  case object UndergraduateAwardHonours extends ProgressionDecisionOutcome(Set("UA1", "UA1-D"))
  case object UndergraduateAwardPass extends ProgressionDecisionOutcome(Set("UA2", "UA2-D"))
  case object UndergraduateAwardDiploma extends ProgressionDecisionOutcome(Set("UA3"))
  case object UndergraduateAwardCertificate extends ProgressionDecisionOutcome(Set("UA4", "UA4-D"))
  case object UndergraduateProceedHonours extends ProgressionDecisionOutcome(Set("UP1", "UP1-S"))
  case object UndergraduateProceedPass extends ProgressionDecisionOutcome(Set("UP2", "UP2-S"))
  case object UndergraduateProceedLevel1 extends ProgressionDecisionOutcome(Set("UP3"))
  case object UndergraduateFinalistAcademicFail extends ProgressionDecisionOutcome(Set("UF1", "UF1-D"))
  case object UndergraduateNonFinalistWithdraw extends ProgressionDecisionOutcome(Set("UF2", "UF2-S"))
  case object UndergraduateResitInSeptember extends ProgressionDecisionOutcome(Set("UR1"))
  case object UndergraduateWithdrawOrResit extends ProgressionDecisionOutcome(Set("UR2", "UR2-S"))
  case object UndergraduateResitWithoutResidence extends ProgressionDecisionOutcome(Set("UR3", "UR3-S"))
  case object UndergraduateResitWithResidence extends ProgressionDecisionOutcome(Set("UR4", "UR4-S"))
  case object UndergraduateFirstSitInSeptember extends ProgressionDecisionOutcome(Set("US1"))
  case object UndergraduateFirstSitWithoutResidence extends ProgressionDecisionOutcome(Set("US2", "US2-S"))
  case object UndergraduateFirstSitWithResidence extends ProgressionDecisionOutcome(Set("US3", "US3-S"))

  def forPitCode(pitCode: String): ProgressionDecisionOutcome =
    values.find(_.pitCodes.contains(pitCode)).getOrElse(throw new NoSuchElementException)

  override def values: IndexedSeq[ProgressionDecisionOutcome] = findValues
}

class ProgressionDecisionOutcomeUserType extends EnumUserType(ProgressionDecisionOutcome)
