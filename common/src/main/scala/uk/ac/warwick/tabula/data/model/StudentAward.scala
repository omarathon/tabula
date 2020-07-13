package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports.{JSet, _}
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
  @JoinColumn(name = "awardCode", referencedColumnName = "code", nullable = false)
  var award: Award = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classificationCode", referencedColumnName = "code", nullable = false)
  var classification: Classification = _

  @Column(name = "award_date")
  var awardDate: DateTime = _


  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "ProgressionDecision_StudentAward",
    joinColumns = Array(new JoinColumn(name = "student_award_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "progression_decision_id", insertable = false, updatable = false))
  )
  @JoinColumn(name = "progression_decision_id", insertable = false, updatable = false)
  @BatchSize(size = 200)
  var _allProgressionDecision: JSet[ProgressionDecision] = JHashSet()


  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "academicYear" -> academicYear,
    "award" -> award,
    "classification" -> classification,
    "awardDate" -> awardDate
  )
}