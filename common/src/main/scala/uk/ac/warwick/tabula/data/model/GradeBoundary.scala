package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence.{Column, Entity}
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.tabula.ToString

object GradeBoundary {
  def apply(marksCode: String, process: GradeBoundaryProcess, attempt: Int, rank: Int, grade: String, minimumMark: Option[Int], maximumMark: Option[Int], signalStatus: GradeBoundarySignalStatus, result: Option[ModuleResult], agreedStatus: GradeBoundaryAgreedStatus, incrementsAttempt: Boolean): GradeBoundary = {
    require(minimumMark.nonEmpty == maximumMark.nonEmpty, "Either both minimum mark and maxmimum mark must be provided, or neither")

    val gb = new GradeBoundary
    gb.grade = grade
    gb.process = process
    gb.attempt = attempt
    gb.rank = rank
    gb.marksCode = marksCode
    gb.minimumMark = minimumMark
    gb.maximumMark = maximumMark
    gb.signalStatus = signalStatus
    gb.result = result
    gb.agreedStatus = agreedStatus
    gb.incrementsAttempt = incrementsAttempt
    gb
  }

  /**
   * The value of the grade field (accompanied with a mark of 0) that should be set if a student did not take a component
   * due to withdrawal.
   */
  val WithdrawnGrade = "W"

  /**
   * The value of the grade field that should be set if a component is missing due to force majeure under regulation 41.
   * @see https://warwick.ac.uk/insite/coronavirus/staff/teaching/marksandexamboards/guidance/marks/#missingmarks
   */
  val ForceMajeureMissingComponentGrade = "FM"

  /**
   * The value of the grade field (with any mark) that should be set if a student's component should be waived due to
   * mitigating circumstances.
   */
  val MitigatingCircumstancesGrade = "M"

  implicit val defaultOrdering: Ordering[GradeBoundary] = Ordering.by { gb: GradeBoundary => (gb.rank, !gb.isDefault, gb.grade) }
}

@Entity
@Proxy
class GradeBoundary extends GeneratedId with ToString {

  @NotNull
  var marksCode: String = _

  @NotNull
  @Type(`type` = "uk.ac.warwick.tabula.data.model.GradeBoundaryProcessUserType")
  var process: GradeBoundaryProcess = _

  @NotNull
  var attempt: Int = _ // e.g. 1 for first attempt, 2 for second attempt, 3 for third attempt

  var rank: Int = _

  @NotNull
  var grade: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var minimumMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var maximumMark: Option[Int] = None

  @NotNull
  @Type(`type` = "uk.ac.warwick.tabula.data.model.GradeBoundarySignalStatusUserType")
  var signalStatus: GradeBoundarySignalStatus = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleResultUserType")
  @Column(name = "result")
  private var _result: ModuleResult = _
  def result: Option[ModuleResult] = Option(_result)
  def result_=(r: Option[ModuleResult]): Unit = _result = r.orNull

  @NotNull
  @Type(`type` = "uk.ac.warwick.tabula.data.model.GradeBoundaryAgreedStatusUserType")
  var agreedStatus: GradeBoundaryAgreedStatus = _

  def generatesResit: Boolean = agreedStatus == GradeBoundaryAgreedStatus.Reassessment

  @NotNull
  var incrementsAttempt: Boolean = _

  def isDefault: Boolean = signalStatus.isDefaultCandidate

  def isValidForMark(mark: Option[Int]): Boolean =
    (minimumMark.isEmpty && maximumMark.isEmpty) ||
    mark.exists { m => minimumMark.exists(_ <= m) && maximumMark.exists(_ >= m) }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "marksCode" -> marksCode,
    "process" -> process,
    "attempt" -> attempt,
    "rank" -> rank,
    "grade" -> grade,
    "result" -> result,
    "minimumMark" -> minimumMark,
    "maximumMark" -> maximumMark,
    "signalStatus" -> signalStatus,
    "isDefault" -> isDefault,
    "agreedStatus" -> agreedStatus,
    "generatesResit" -> generatesResit,
    "incrementsAttempt" -> incrementsAttempt,
  )
}

sealed abstract class GradeBoundaryProcess(override val entryName: String) extends EnumEntry
object GradeBoundaryProcess extends Enum[GradeBoundaryProcess] {
  // We only import StudentAssessment and Reassessment
  case object StudentAssessment extends GradeBoundaryProcess("SAS")
  case object Reassessment extends GradeBoundaryProcess("RAS")

  case object LateAssessment extends GradeBoundaryProcess("LAS")
  case object IndividualAssessment extends GradeBoundaryProcess("IAS")
  case object Other extends GradeBoundaryProcess("OTH")

  override def values: IndexedSeq[GradeBoundaryProcess] = findValues
}

class GradeBoundaryProcessUserType extends EnumUserType(GradeBoundaryProcess)

// "Sig Sta" https://www.mysits.com/mysits/sits990/990manuals/index.htm?https://www.mysits.com/mysits/sits990/990manuals/cams/05asspro/08prereq/04mkc.htm
sealed abstract class GradeBoundarySignalStatus(override val entryName: String, val isDefaultCandidate: Boolean) extends EnumEntry
object GradeBoundarySignalStatus extends Enum[GradeBoundarySignalStatus] {
  // No signal, i.e. there are no special instructions associated with this mark and grade combination.
  case object NoSignal extends GradeBoundarySignalStatus("N", isDefaultCandidate = true)

  // Qualified failure.  The Calculate process automatically sends a Q signal if the student does not reach the qualifying mark for an assessment item
  // (as defined on the Module Assessment Body (MAB) record).  Therefore, users only need to set up mark and grade combinations with a Q Signal Status for the
  // Module Marking Scheme.  Q signals may be used to force students to be re-assessed even if their overall module result is a pass.
  case object QualifiedFailure extends GradeBoundarySignalStatus("Q", isDefaultCandidate = true)

  // Signals force a specific outcome which cannot be overridden at the calculation stage.  The different types of signals (e.g. plagiarism, medical
  // certificate) are defined within the Mark Scheme Signal (MSS) table, and attached to the Mark Conversion record via the Send or Receive Signal fields.
  // Assessments send signals and module results receive signals.
  case object SpecificOutcome extends GradeBoundarySignalStatus("S", isDefaultCandidate = false)

  override def values: IndexedSeq[GradeBoundarySignalStatus] = findValues
}

class GradeBoundarySignalStatusUserType extends EnumUserType(GradeBoundarySignalStatus)

// "Agr Sta" https://www.mysits.com/mysits/sits990/990manuals/index.htm?https://www.mysits.com/mysits/sits990/990manuals/cams/05asspro/08prereq/04mkc.htm
sealed abstract class GradeBoundaryAgreedStatus(override val entryName: String) extends EnumEntry
object GradeBoundaryAgreedStatus extends Enum[GradeBoundaryAgreedStatus] {
  // Agreed – the record is complete
  case object Agreed extends GradeBoundaryAgreedStatus("A")

  // Held - the result cannot be finalised or sent into another process. This status is normally picked up from as S type signal result (e.g. plagiarism).
  // The Held status is only a temporary state.
  case object Held extends GradeBoundaryAgreedStatus("H")

  case object Late extends GradeBoundaryAgreedStatus("L")
  case object Reassessment extends GradeBoundaryAgreedStatus("R")

  override def values: IndexedSeq[GradeBoundaryAgreedStatus] = findValues
}

class GradeBoundaryAgreedStatusUserType extends EnumUserType(GradeBoundaryAgreedStatus)
