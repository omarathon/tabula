package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarking
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="SeenSecondMarking")
class SeenSecondMarkingWorkflow extends MarkingWorkflow with AssignmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = SeenSecondMarking

	def onlineMarkingUrl(assignment: Assignment, marker: User) = MarkingRoutes.onlineMarkerFeedback(assignment)

	def hasSecondMarker = true

}
