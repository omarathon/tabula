package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarkingNew
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.Routes

@Entity
@DiscriminatorValue(value="SeenSecondMarkingNew")
class SeenSecondMarkingNewWorkflow extends MarkingWorkflow with AssignmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = SeenSecondMarkingNew

	// actually there will be 2 different views for the first marker, depending on the stage in the workflow
	def onlineMarkingUrl(assignment: Assignment, marker: User) =
		if (assignment.isFirstMarker(marker)) Routes.coursework.admin.assignment.onlineMarkerFeedback(assignment)
		else Routes.coursework.admin.assignment.onlineModeration(assignment)

	override def firstMarkerRoleName: String = "First marker"
	def hasSecondMarker = true
	def secondMarkerRoleName = Some("Second marker")
	def secondMarkerVerb = Some("mark")
}
