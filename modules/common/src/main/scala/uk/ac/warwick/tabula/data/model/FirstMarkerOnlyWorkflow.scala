package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.userlookup.User
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.MarkingMethod.FirstMarkerOnly
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.Routes

@Entity
@DiscriminatorValue(value="FirstMarkerOnly")
class FirstMarkerOnlyWorkflow extends MarkingWorkflow with NoSecondMarker with NoThirdMarker with AssignmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	@transient var submissionService = Wire[SubmissionService]

	def markingMethod = FirstMarkerOnly

	def onlineMarkingUrl(assignment: Assignment, marker: User) = Routes.coursework.admin.assignment.onlineMarkerFeedback(assignment)

	override def getStudentsSecondMarker(assignment: Assignment, universityId: String) = None

}