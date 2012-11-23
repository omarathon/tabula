package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.annotations.Type
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.JoinColumn
import scala.reflect.BeanProperty
import javax.persistence.OneToOne
import javax.persistence.CascadeType
import javax.persistence.Entity
import uk.ac.warwick.tabula.data.PreSaveBehaviour


/**
 * An assessment group is basically the smallest group of people
 * who would be submitting to the same assignment. It is identified
 * by four pieces of information:
 *
 * - The module it's in
 * - The academic year
 * - The assessment group code (1 or more assignments in the module will share this code)
 * - The occurrence (Splits a module up into multiple occurrences/cohorts per year - 99% of the time
 *          there's only one value but sometimes can be 2 or even 26 in one case)
 */
@Entity
class UpstreamAssessmentGroup extends GeneratedId with PreSaveBehaviour {
	var moduleCode: String = _
	var assessmentGroup: String = _
	var occurrence: String = _

	@Basic @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = _

	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "membersgroup_id")
	@BeanProperty var members: UserGroup = UserGroup.emptyUniversityIds

	override def preSave(newRecord: Boolean) {
		if (!members.universityIds) throw new IllegalStateException
	}

	/**
	 * A short textual identifier for this group, useful for logging.
	 */
	def toText = "%s %s g:%s o:%s" format (moduleCode, academicYear, assessmentGroup, occurrence)
}