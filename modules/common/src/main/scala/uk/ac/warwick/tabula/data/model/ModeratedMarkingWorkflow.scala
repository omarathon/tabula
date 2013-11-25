package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.ModeratedMarking
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="ModeratedMarking")
class ModeratedMarkingWorkflow extends MarkingWorkflow with AssignmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = ModeratedMarking

	def onlineMarkingUrl(assignment: Assignment, marker: User) = assignment.isFirstMarker(marker) match {
		case true => MarkingRoutes.onlineMarkerFeedback(assignment)
		case false => MarkingRoutes.onlineModeration(assignment)
	}

	// True if this marking workflow uses a second marker
	def hasSecondMarker = true

}
