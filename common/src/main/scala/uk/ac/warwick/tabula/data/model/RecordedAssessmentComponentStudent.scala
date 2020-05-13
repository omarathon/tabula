package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._

/**
 * A set of marks for a student on a particular AssessmentComponent, as recorded in Tabula. This is
 * *NOT* canonical, the canonical mark should use the fields on UpstreamAssessmentGroupMember, but it
 * is guaranteed to be persistent - it will not be removed if the marks record is removed from SITS nor
 * will it be updated by updates in SITS; it is a permanent record of marks that have been recorded in
 * Tabula for upload into SITS.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedAssessmentComponentStudent extends GeneratedId
  with HibernateVersioned
  with ToString {

  def this(uagm: UpstreamAssessmentGroupMember) {
    this()
    this.moduleCode = uagm.upstreamAssessmentGroup.moduleCode
    this.assessmentGroup = uagm.upstreamAssessmentGroup.assessmentGroup
    this.occurrence = uagm.upstreamAssessmentGroup.occurrence
    this.sequence = uagm.upstreamAssessmentGroup.sequence
    this.academicYear = uagm.upstreamAssessmentGroup.academicYear
    this.universityId = uagm.universityId
  }

  // Properties for linking through to AssessmentComponent/UpstreamAssessmentGroup/UpstreamAssessmentGroupMember

  // Long-form module code with hyphen and CATS value
  @Column(name = "module_code", nullable = false)
  var moduleCode: String = _

  @Column(name = "assessment_group", nullable = false)
  var assessmentGroup: String = _

  @Column(nullable = false)
  var occurrence: String = _

  @Column(nullable = false)
  var sequence: String = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(name = "university_id", nullable = false)
  var universityId: String = _

  @OneToMany(mappedBy = "recordedAssessmentComponentStudent", cascade = Array(ALL), fetch = FetchType.LAZY)
  @OrderBy("updated_date DESC")
  @BatchSize(size = 200)
  private val _marks: JList[RecordedAssessmentComponentStudentMark] = JArrayList()
  def marks: Seq[RecordedAssessmentComponentStudentMark] = _marks.asScala.toSeq

  def addMark(uploaderId: String, mark: Int, grade: Option[String], comments: String = null): RecordedAssessmentComponentStudentMark = {
    val newMark = new RecordedAssessmentComponentStudentMark
    newMark.recordedAssessmentComponentStudent = this
    newMark.mark = mark
    newMark.grade = grade
    newMark.comments = comments
    newMark.updatedBy = uploaderId
    newMark.updatedDate = DateTime.now
    _marks.add(0, newMark) // add at the top as we know it's the latest one, the rest get shifted down
    needsWritingToSits = true
    newMark
  }

  def latestMark: Option[Int] = marks.headOption.map(_.mark)
  def latestGrade: Option[String] = marks.headOption.flatMap(_.grade)

  @Column(name = "needs_writing_to_sits", nullable = false)
  var needsWritingToSits: Boolean = false

  // empty for any student that's never been written
  @Column(name = "last_written_to_sits")
  private var _lastWrittenToSits: DateTime = _
  def lastWrittenToSits: Option[DateTime] = Option(_lastWrittenToSits)
  def lastWrittenToSits_=(lastWrittenToSits: Option[DateTime]): Unit = _lastWrittenToSits = lastWrittenToSits.orNull

  override def toStringProps: Seq[(String, Any)] = Seq(
    "moduleCode" -> moduleCode,
    "assessmentGroup" -> assessmentGroup,
    "occurrence" -> occurrence,
    "sequence" -> sequence,
    "academicYear" -> academicYear,
    "universityId" -> universityId,
    "marks" -> marks,
    "needsWritingToSits" -> needsWritingToSits,
    "lastWrittenToSits" -> lastWrittenToSits
  )
}

@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedAssessmentComponentStudentMark extends GeneratedId
  with ToString {

  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = Array())
  @JoinColumn(name = "recorded_assessment_component_student_id", nullable = false)
  @ForeignKey(name = "none")
  var recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent = _

  @Column(nullable = false)
  var mark: Int = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var grade: Option[String] = None

  var comments: String = _

  @Column(name = "updated_by", nullable = false)
  var updatedBy: String = _

  @Column(name = "updated_date", nullable = false)
  var updatedDate: DateTime = _

  override def toStringProps: Seq[(String, Any)] = Seq(
    "mark" -> mark,
    "grade" -> grade,
    "comments" -> comments
  )
}
