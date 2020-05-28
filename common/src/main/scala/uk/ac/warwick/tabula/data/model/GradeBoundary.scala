package uk.ac.warwick.tabula.data.model

import javax.persistence.Entity
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}

object GradeBoundary {
  def apply(marksCode: String, grade: String, minimumMark: Option[Int], maximumMark: Option[Int], signalStatus: String): GradeBoundary = {
    require(minimumMark.nonEmpty == maximumMark.nonEmpty, "Either both minimum mark and maxmimum mark must be provided, or neither")

    val gb = new GradeBoundary()
    gb.grade = grade
    gb.marksCode = marksCode
    gb.minimumMark = minimumMark
    gb.maximumMark = maximumMark
    gb.signalStatus = signalStatus
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

  private val byGradeOrdering = Ordering.by[GradeBoundary, String](_.grade)

  implicit val defaultOrdering = new Ordering[GradeBoundary]() {
    override def compare(x: GradeBoundary, y: GradeBoundary): Int = {
      if (x.isDefault && y.isDefault) byGradeOrdering.compare(x, y)
      else if (x.isDefault) -1
      else if (y.isDefault) 1
      else byGradeOrdering.compare(x, y)
    }
  }
}

@Entity
@Proxy
class GradeBoundary extends GeneratedId {

  @NotNull
  var marksCode: String = _

  @NotNull
  var grade: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var minimumMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var maximumMark: Option[Int] = None

  @NotNull
  var signalStatus: String = _

  def isDefault: Boolean = signalStatus == "N"

  def isValidForMark(mark: Option[Int]): Boolean =
    (minimumMark.isEmpty && maximumMark.isEmpty) ||
    mark.exists { m => minimumMark.exists(_ <= m) && maximumMark.exists(_ >= m) }

}
