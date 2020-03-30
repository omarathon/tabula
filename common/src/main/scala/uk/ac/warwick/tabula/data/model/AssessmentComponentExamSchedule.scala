package uk.ac.warwick.tabula.data.model

import javax.persistence.{Basic, Column, Entity}
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.{AcademicYear, ToString}

@Entity
@Proxy
class AssessmentComponentExamSchedule extends GeneratedId with ToString {

  def this(examProfileCode: String, slotId: String, sequence: String) {
    this()
    this.examProfileCode = examProfileCode
    this.slotId = slotId
    this.sequence = sequence
  }

  // moduleCode and assessmentComponentSequence are a many-to-one mapping to an AssessmentComponent
  @Column(name = "module_code", nullable = false)
  var moduleCode: String = _

  @Column(name = "assessmentcomponent_sequence", nullable = false)
  var assessmentComponentSequence: String = _

  // A schedule is unique on examProfileCode, slotId and sequence
  @Column(name = "profile_code", nullable = false)
  var examProfileCode: String = _

  @Column(name = "slot_id", nullable = false)
  var slotId: String = _

  @Column(name = "sequence", nullable = false)
  var sequence: String = _

  // This is the academic year of the users on the assessment, used for linking to UpstreamAssessmentGroup (along with moduleCode and assessmentComponentSequence)
  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(name = "start_time", nullable = false)
  var startTime: DateTime = _

  @Column(name = "exam_paper_code", nullable = false)
  var examPaperCode: String = _

  @Column(name = "exam_paper_section", nullable = true)
  private var _examPaperSection: String = _
  def examPaperSection: Option[String] = _examPaperSection.maybeText
  def examPaperSection_=(section: Option[String]): Unit = _examPaperSection = section.orNull

  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  @Column(name = "location", nullable = true)
  private var _location: Location = _
  def location: Option[Location] = Option(_location)
  def location_=(location: Option[Location]): Unit = _location = location.orNull

  def copyFrom(other: AssessmentComponentExamSchedule): AssessmentComponentExamSchedule = {
    require(other.id == null, "Can only copy from transient instances")
    require(other.examProfileCode == examProfileCode && other.slotId == slotId && other.sequence == sequence, "Must be for the same exam profile, slot and sequence in slot")
    this.moduleCode = other.moduleCode
    this.assessmentComponentSequence = other.assessmentComponentSequence
    this.academicYear = other.academicYear
    this.startTime = other.startTime
    this.examPaperCode = other.examPaperCode
    this._examPaperSection = other._examPaperSection
    this._location = other._location
    this
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "moduleCode" -> moduleCode,
    "assessmentComponentSequence" -> assessmentComponentSequence,
    "examProfileCode" -> examProfileCode,
    "slotId" -> slotId,
    "sequence" -> sequence,
    "academicYear" -> academicYear,
    "startTime" -> startTime,
    "examPaperCode" -> examPaperCode,
    "examPaperSection" -> _examPaperSection,
    "location" -> _location
  )
}
