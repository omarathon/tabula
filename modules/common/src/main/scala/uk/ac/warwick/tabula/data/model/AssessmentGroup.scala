package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef

@Entity
class AssessmentGroup extends GeneratedId {

	@ManyToOne
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	@ManyToOne
	@JoinColumn(name = "upstream_id")
	var upstreamAssignment: UpstreamAssignment = _

	var occurrence: String = _
	
	override def toString = "assessmentGroup: { assignment: " +
													assignment.id + " (" + assignment.name + "), upstreamAssignment: " +
													upstreamAssignment.id + " (" + upstreamAssignment.name + "), occurrence: " + occurrence + " }"

}
