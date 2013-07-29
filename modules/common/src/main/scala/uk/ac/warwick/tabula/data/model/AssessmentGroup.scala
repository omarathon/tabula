package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

@Entity
class AssessmentGroup extends GeneratedId {

	@ManyToOne
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	@ManyToOne
	@JoinColumn(name = "group_set_id")
	var smallGroupSet: SmallGroupSet = _

	@ManyToOne
	@JoinColumn(name = "upstream_id")
	var upstreamAssignment: UpstreamAssignment = _

	var occurrence: String = _

	override def toString = {
		if (assignment != null && upstreamAssignment != null && occurrence != null) {
			"assessmentGroup: { assignment: " + assignment.id + " (" + assignment.name +
			"), upstreamAssignment: " + upstreamAssignment.id + " (" + upstreamAssignment.name +
			"), occurrence: " + occurrence + " }"
		} else {
			"assessmentGroup: (invalid, contains nulls)"
		}
	}
}