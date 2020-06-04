package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

/**
 * A set of module marks for a student on a particular Module, as recorded in Tabula. This is
 * *NOT* the canonical module mark, the canonical mark should use the fields on ModuleRegistration, but it
 * is guaranteed to be persistent - it will not be removed if the module registration record is removed from SITS nor
 * will it be updated by updates in SITS; it is a permanent record of module marks that have been recorded in
 * Tabula for upload into SITS.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedModuleRegistration extends GeneratedId
  with HibernateVersioned
  with ToString {

  def this(mr: ModuleRegistration) {
    this()
    this.scjCode = mr._scjCode
    this.module = mr.module
    this.cats = mr.cats
    this.academicYear = mr.academicYear
    this.occurrence = mr.occurrence
  }

  @Column(name = "scj_code", nullable = false)
  var scjCode: String = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "module_code", referencedColumnName = "code")
  var module: Module = _

  @Column(nullable = false)
  var cats: JBigDecimal = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(nullable = false)
  var occurrence: String = _

  @OneToMany(mappedBy = "recordedModuleRegistration", cascade = Array(ALL), fetch = FetchType.LAZY)
  @OrderBy("updated_date DESC")
  @BatchSize(size = 200)
  private val _marks: JList[RecordedModuleMark] = JArrayList()
  def marks: Seq[RecordedModuleMark] = _marks.asScala.toSeq

  def addMark(
    uploader: User,
    mark: Option[Int],
    grade: Option[String],
    result: Option[ModuleResult],
    source: RecordedModuleMarkSource,
    markState: MarkState,
    comments: String = null
  ): RecordedModuleMark = {
    val newMark = new RecordedModuleMark
    newMark.recordedModuleRegistration = this
    newMark.mark = mark
    newMark.grade = grade
    newMark.result = result
    newMark.comments = comments
    newMark.source = source
    newMark.markState = markState
    newMark.updatedBy = uploader
    newMark.updatedDate = DateTime.now
    _marks.add(0, newMark) // add at the top as we know it's the latest one, the rest get shifted down
    needsWritingToSits = true
    newMark
  }

  def latestMark: Option[Int] = marks.headOption.flatMap(_.mark)
  def latestGrade: Option[String] = marks.headOption.flatMap(_.grade)
  def latestResult: Option[ModuleResult] = marks.headOption.flatMap(_.result)
  def latestState: Option[MarkState] = marks.headOption.map(_.markState)

  @Column(name = "needs_writing_to_sits", nullable = false)
  var needsWritingToSits: Boolean = false

  // empty for any student that's never been written
  @Column(name = "last_written_to_sits")
  private var _lastWrittenToSits: DateTime = _
  def lastWrittenToSits: Option[DateTime] = Option(_lastWrittenToSits)
  def lastWrittenToSits_=(lastWrittenToSits: Option[DateTime]): Unit = _lastWrittenToSits = lastWrittenToSits.orNull

  override def toStringProps: Seq[(String, Any)] = Seq(
    "scjCode" -> scjCode,
    "moduleCode" -> module.code,
    "cats" -> cats,
    "academicYear" -> academicYear,
    "occurrence" -> occurrence,
  )

}


@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedModuleMark extends GeneratedId
  with ToString {

  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = Array())
  @JoinColumn(name = "recorded_module_registration_id", nullable = false)
  @ForeignKey(name = "none")
  var recordedModuleRegistration: RecordedModuleRegistration = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var mark: Option[Int] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var grade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionModuleResultUserType")
  @Column(name = "module_result")
  var result: Option[ModuleResult] = _

  var comments: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.RecordedModuleMarkSourceUserType")
  var source: RecordedModuleMarkSource = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.MarkStateUserType")
  @Column(name = "mark_state")
  var markState: MarkState = UnconfirmedActual

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  @Column(name = "updated_by", nullable = false)
  var updatedBy: User = _

  @Column(name = "updated_date", nullable = false)
  var updatedDate: DateTime = _

  override def toStringProps: Seq[(String, Any)] = Seq(
    "mark" -> mark,
    "grade" -> grade,
    "result" -> result,
    "comments" -> comments,
    "source" -> source
  )
}

sealed trait RecordedModuleMarkSource extends EnumEntry
object RecordedModuleMarkSource extends Enum[RecordedModuleMarkSource] {
  case object ComponentMarkCalculation extends RecordedModuleMarkSource
  case object MarkConfirmation extends RecordedModuleMarkSource

  override def values: IndexedSeq[RecordedModuleMarkSource] = findValues
}

class RecordedModuleMarkSourceUserType extends EnumUserType(RecordedModuleMarkSource)
