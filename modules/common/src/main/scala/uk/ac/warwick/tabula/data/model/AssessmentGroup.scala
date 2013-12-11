package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.spring.Wire

/**
 * This entity is basically a many-to-many mapper between
 * assignment/smallgroupset and assessmentcomponent, so
 * that they can link to multiple assessmentcomponents.
 *
 * It is not directly related to UpstreamAssessmentGroup
 * as the name might suggest - it is a confusing name.
 */
@Entity
class AssessmentGroup extends GeneratedId {

	/*
	Either assignment _or_ smallGroupSet will be non-null
	depending on which type of entity we're linking an
	AssessmentComponent to...
	 */
	@transient var membershipService = Wire[AssignmentMembershipService]

	@ManyToOne
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	@ManyToOne
	@JoinColumn(name = "group_set_id")
	var smallGroupSet: SmallGroupSet = _

	@ManyToOne
	@JoinColumn(name = "upstream_id")
	var assessmentComponent: AssessmentComponent = _

	var occurrence: String = _
	
	def toUpstreamAssessmentGroup(academicYear: AcademicYear): Option[UpstreamAssessmentGroup] =
		if (academicYear == null || assessmentComponent == null || occurrence == null) {
			None
		} else {
			val template = new UpstreamAssessmentGroup
			template.academicYear = academicYear
			template.assessmentGroup = assessmentComponent.assessmentGroup
			template.moduleCode = assessmentComponent.moduleCode
			template.occurrence = occurrence
			membershipService.getUpstreamAssessmentGroup(template)
		}

	override def toString = {
		if ((assignment != null || smallGroupSet != null) && assessmentComponent != null && occurrence != null) {

			val entityInfo =
				if (assignment != null) Seq("assignment" -> assignment.id)
				else Seq("smallGroupSet" -> smallGroupSet.id)

			val props = entityInfo ++ Seq(
				"assessmentComponent" -> assessmentComponent.id,
				"occurrence" -> occurrence
			)

			ToString.forObject(this, props : _*)

		} else {
			"AssessmentGroup(invalid, contains nulls)"
		}
	}
}