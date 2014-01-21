package uk.ac.warwick.tabula.data.model

import javax.persistence.{Column, Table, Entity}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import org.hibernate.annotations.Type
import javax.persistence.FetchType
import uk.ac.warwick.tabula.JavaImports._
import javax.persistence.JoinColumns
import javax.persistence.JoinColumn
import javax.persistence.OneToMany


/**
 * Represents an upstream assessment component as found in the central
 * University systems. An component is timeless - it doesn't
 * relate to a specific instance of an assignment/exam or even a particular year.
 *
 * This used to be UpstreamAssignment when we were only importing assignment-type
 * components. Now we include other things like exams, so it has been renamed
 * AssessmentComponent in line with what it's called in SITS.
 */
@Entity
@Table(name="UPSTREAMASSIGNMENT")
class AssessmentComponent extends GeneratedId with PreSaveBehaviour {

	@transient var membershipService = Wire.auto[AssignmentMembershipService]

	/**
	 * Lowercase module code, with CATS. e.g. in304-15
	 */
	var moduleCode: String = _
	/**
	 * Assessment group the assignment is in. Is mostly a meaningless
	 * character but will map to a corresponding student module registratation
	 * to the same group.
	 */
	var assessmentGroup: String = _
	/**
	 * Identifier for the assignment, unique within a given moduleCode.
	 */
	var sequence: String = _
	/**
	 * Lowercase department code, e.g. md.
	 */
	var departmentCode: String = _

	/**
	 * Name as defined upstream.
	 */
	var name: String = _

	/**
	 * The type of component. Typical values are A for assignment,
	 * E for summer exam. Other values exist.
	 */
	@Type(`type`="uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
	@Column(nullable=false)
	var assessmentType: AssessmentType = _
	
	/**
	 * Read-only mapping of upstream groups. Used by AssignmentMembershipDao to inform Hibernate of how to join properly
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns(Array(
    new JoinColumn(name = "moduleCode", referencedColumnName = "moduleCode", insertable = false, updatable = false),
    new JoinColumn(name = "assessmentGroup", referencedColumnName = "assessmentGroup", insertable = false, updatable = false)
  ))
	var upstreamAssessmentGroups: JSet[UpstreamAssessmentGroup] = _

	/**
	 * Returns moduleCode without CATS. e.g. in304
	 */
	def moduleCodeBasic: String = Module.stripCats(moduleCode)

	/**
	 * Returns the CATS as a string if it's present, e.g. 50
	 */
	def cats: Option[String] = Module.extractCats(moduleCode)

	def needsUpdatingFrom(other: AssessmentComponent) = (
		this.name != other.name ||
		this.departmentCode != other.departmentCode)

	override def preSave(newRecord: Boolean) {
		ensureNotNull("name", name)
		ensureNotNull("moduleCode", moduleCode)
	}

	private def ensureNotNull(name: String, value: Any) {
		if (value == null) throw new IllegalStateException("null " + name + " not allowed")
	}

	def copyFrom(other: AssessmentComponent) {
		moduleCode = other.moduleCode
		assessmentGroup = other.assessmentGroup
		sequence = other.sequence
		departmentCode = other.departmentCode
		name = other.name
		assessmentType = other.assessmentType
	}
	
	def upstreamAssessmentGroups(year: AcademicYear) = membershipService.getUpstreamAssessmentGroups(this, year)
}

object AssessmentComponent {
	/** The value we store as the assessment group when no group has been chosen for this student
	  * (in ADS it is null)
	  *
	  * We also use this value for a few other fields, like Occurrence, so the variable
	  * name is a bit too specific.
	  */
	val NoneAssessmentGroup = "NONE"
}