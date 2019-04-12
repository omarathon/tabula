package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence.{Access, AccessType, Basic, Column, Entity, FetchType, JoinColumn, ManyToOne}
import org.hibernate.annotations.Type
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.tabula.data.model.{AssessmentType, GeneratedId, Module}
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
@Access(AccessType.FIELD)
class MitigatingCircumstancesAffectedAssessment extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable {

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
