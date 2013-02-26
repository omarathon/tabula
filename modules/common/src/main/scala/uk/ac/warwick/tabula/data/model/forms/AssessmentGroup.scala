package uk.ac.warwick.tabula.data.model.forms

import javax.persistence._
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.{GeneratedId, Assignment, UpstreamAssignment}

@Entity
class AssessmentGroup extends GeneratedId {

	@ManyToOne
	@JoinColumn(name = "assignment_id")
	@BeanProperty var assignment: Assignment = _

	@ManyToOne
	@JoinColumn(name = "upstream_id")
	@BeanProperty var upstreamAssignment: UpstreamAssignment = _

	@BeanProperty var occurrence: String = _

}
