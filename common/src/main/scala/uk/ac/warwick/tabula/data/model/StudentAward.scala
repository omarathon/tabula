package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.{AcademicYear, ToString}

@Entity
@Proxy
@Access(AccessType.FIELD)
class StudentAward extends GeneratedId with ToString {

  @Column(name = "spr_code", nullable = false)
  var sprCode: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "award_code", referencedColumnName = "code", nullable = false)
  var award: Award = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classification_code", referencedColumnName = "code")
  private var _classification: Classification = _
  def classification: Option[Classification] = Option(_classification)
  def classification_=(classification: Option[Classification]): Unit  = _classification = classification.orNull

  @Column(name = "award_date")
  private var _awardDate: LocalDate = _
  def awardDate: Option[LocalDate] = Option(_awardDate)
  def awardDate_=(awardDate: Option[LocalDate]): Unit = _awardDate = awardDate.orNull

  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "academicYear" -> academicYear,
    "award" -> award,
    "classification" -> classification,
    "awardDate" -> awardDate
  )
}
