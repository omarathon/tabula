package uk.ac.warwick.tabula.data.model

import javax.persistence.{Column, Entity}
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}

object GradeBoundary {
  def apply(marksCode: String, process: String, rank: Int, grade: String, minimumMark: Option[Int], maximumMark: Option[Int], signalStatus: String, result: Option[ModuleResult]): GradeBoundary = {
    require(minimumMark.nonEmpty == maximumMark.nonEmpty, "Either both minimum mark and maxmimum mark must be provided, or neither")

    val gb = new GradeBoundary
    gb.grade = grade
    gb.process = process
    gb.rank = rank
    gb.marksCode = marksCode
    gb.minimumMark = minimumMark
    gb.maximumMark = maximumMark
    gb.signalStatus = signalStatus
    gb.result = result
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
class GradeBoundary extends GeneratedId {

  @NotNull
  var marksCode: String = _

  @NotNull
  var process: String = _ // SAS (first sit), RAS (resit), LAS (late), IAS (individual???), OTH (other). We only import SAS and RAS

  var rank: Int = _

  @NotNull
  var grade: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var minimumMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var maximumMark: Option[Int] = None

  @NotNull
  var signalStatus: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.ModuleResultUserType")
  @Column(name = "result")
  private var _result: ModuleResult = _
  def result: Option[ModuleResult] = Option(_result)
  def result_=(r: Option[ModuleResult]): Unit = _result = r.orNull

  def isDefault: Boolean = signalStatus == "N"

  def isValidForMark(mark: Option[Int]): Boolean =
    (minimumMark.isEmpty && maximumMark.isEmpty) ||
    mark.exists { m => minimumMark.exists(_ <= m) && maximumMark.exists(_ >= m) }

}
