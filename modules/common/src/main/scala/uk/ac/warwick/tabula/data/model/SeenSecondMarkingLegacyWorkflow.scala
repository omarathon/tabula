package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarkingLegacy
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.Routes

@Entity
@DiscriminatorValue(value="SeenSecondMarking")
class SeenSecondMarkingLegacyWorkflow extends MarkingWorkflow with NoThirdMarker with AssignmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = SeenSecondMarkingLegacy

	def onlineMarkingUrl(assignment: Assignment, marker: User) = Routes.coursework.admin.assignment.onlineMarkerFeedback(assignment)

	override def firstMarkerRoleName: String = "First marker"
	def hasSecondMarker = true
	def secondMarkerRoleName = Some("Second marker")
	def secondMarkerVerb = Some("mark")
}
