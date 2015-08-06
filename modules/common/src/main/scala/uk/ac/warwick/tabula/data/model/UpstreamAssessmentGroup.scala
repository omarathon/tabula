package uk.ac.warwick.tabula.data.model

import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.annotations.Type
import javax.persistence.{Basic, Column, Entity, JoinColumn, OneToOne}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import javax.persistence.CascadeType
import javax.persistence.FetchType


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

	// Long-form module code with hyphen and CATS value
	var moduleCode: String = _
	var assessmentGroup: String = _
	var occurrence: String = _
	var sequence: String = _

	@Basic @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = _

	@OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	var members: UserGroup = UserGroup.ofUniversityIds

	// UpstreamAssessmentGroups only ever use the static users in the UserGroup
	// you can access this directly to get a list of members by seatNumber
	def sortedMembers = members.sortedStaticMembers

	override def preSave(newRecord: Boolean) {
		if (!members.universityIds) throw new IllegalStateException
	}

	def isEquivalentTo(other: UpstreamAssessmentGroup) =
		new EqualsBuilder()
			.append(moduleCode, other.moduleCode)
			.append(assessmentGroup, other.assessmentGroup)
			.append(occurrence, other.occurrence)
			.append(academicYear, other.academicYear)
			.append(sequence, other.sequence)
			.isEquals

	override def toString = "%s %s g:%s o:%s s:%s" format (moduleCode, academicYear, assessmentGroup, occurrence, sequence)

	def memberCount = members.members.size
}