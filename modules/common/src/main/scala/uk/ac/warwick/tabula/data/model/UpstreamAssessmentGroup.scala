package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import javax.persistence.{Basic, Column, Entity, JoinColumn, OneToOne}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import javax.persistence.CascadeType


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
	var members: UserGroup = UserGroup.ofUniversityIds

	override def preSave(newRecord: Boolean) {
		if (!members.universityIds) throw new IllegalStateException
	}

	override def toString = "%s %s g:%s o:%s" format (moduleCode, academicYear, assessmentGroup, occurrence)
	
	def memberCount = members.members.size
}