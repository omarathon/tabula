package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.JavaImports._


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
class AssessmentComponent extends GeneratedId with PreSaveBehaviour with Serializable {

	@transient var membershipService = Wire.auto[AssignmentMembershipService]

	/**
	 * Uppercase module code, with CATS. e.g. IN304-15
	 */
	var moduleCode: String = _

	/**
	 * An OPTIONAL link to a Tabula representation of a Module.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "module_id")
	var module: Module = _

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
	@deprecated("TAB-2839 Don't make any assumptions on the department code here, the module may have been moved in Tabula", "85")
	def departmentCode = _departmentCode

	@deprecated("TAB-2839 Don't make any assumptions on the department code here, the module may have been moved in Tabula", "85")
	def departmentCode_=(code: String) { _departmentCode = code }

	@Column(name = "DEPARTMENTCODE")
	private var _departmentCode: String = _

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
	 * Read-only mapping of upstream groups. Used by AssignmentMembershipDao to inform Hibernate of how to join properly.
	 * 
	 * Note that this ISN'T really OneToMany
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumns(Array(
    new JoinColumn(name = "moduleCode", referencedColumnName = "moduleCode", insertable = false, updatable = false, unique = false),
    new JoinColumn(name = "assessmentGroup", referencedColumnName = "assessmentGroup", insertable = false, updatable = false, unique = false)
  ))
	var upstreamAssessmentGroups: JSet[UpstreamAssessmentGroup] = _

	/**
	 * Returns moduleCode without CATS. e.g. in304
	 */
	def moduleCodeBasic: String = Module.stripCats(moduleCode).get

	/**
	 * Returns the CATS as a string if it's present, e.g. 50
	 */
	def cats: Option[String] = Module.extractCats(moduleCode)

	def needsUpdatingFrom(other: AssessmentComponent) = (
		this.name != other.name ||
		this._departmentCode != other._departmentCode ||
		this.module != other.module
	)

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
		_departmentCode = other._departmentCode
		module = other.module
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