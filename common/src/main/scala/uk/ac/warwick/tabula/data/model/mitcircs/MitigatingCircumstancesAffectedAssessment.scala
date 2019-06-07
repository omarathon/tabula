package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands.mitcircs.submission.AffectedAssessmentItem
import uk.ac.warwick.tabula.data.model.{AssessmentType, GeneratedId, Module}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.{AcademicYear, ToString}

@Entity
@Proxy
@Access(AccessType.FIELD)
class MitigatingCircumstancesAffectedAssessment extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable {

  def this(_submission: MitigatingCircumstancesSubmission, item: AffectedAssessmentItem) {
    this()
    this.mitigatingCircumstancesSubmission = _submission
    this.moduleCode = item.moduleCode
    this.sequence = item.sequence
    this.module = item.module
    this.academicYear = item.academicYear
    this.name = item.name
    this.assessmentType = item.assessmentType
    this.deadline = item.deadline
  }

  /**
    * Uppercase module code, optionally with CATS. e.g. IN304-15 or IN304.
    *
    * If this has the CATS, it can be mapped to an AssessmentComponent, otherwise it
    * has just been provided by the student.
    */
  @Column(nullable = false)
  var moduleCode: String = _

  /**
    * For linking to an AssessmentComponent
    */
  var sequence: String = _

  /**
    * A link to the Tabula representation of a Module.
    */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "module_id")
  var module: Module = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = _

  /**
    * The name of the assessment or exam
    */
  var name: String = _

  /**
    * The type of component. Typical values are A for assignment,
    * E for summer exam. Other values exist.
    */
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(nullable = false)
  var assessmentType: AssessmentType = _

  @Column(nullable = false)
  var deadline: LocalDate = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  var mitigatingCircumstancesSubmission: MitigatingCircumstancesSubmission = _

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "moduleCode" -> moduleCode,
    "sequence" -> sequence,
    "academicYear" -> academicYear.toString,
    "name" -> name,
    "assessmentType" -> assessmentType.code,
    "deadline" -> deadline,
    "mitigatingCircumstancesSubmission" -> mitigatingCircumstancesSubmission.id,
  )

  override def permissionsParents: Stream[PermissionsTarget] = Stream(mitigatingCircumstancesSubmission)

}
