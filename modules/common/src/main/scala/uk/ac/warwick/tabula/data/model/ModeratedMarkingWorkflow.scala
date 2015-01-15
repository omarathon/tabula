package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.ModeratedMarking
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.Routes
import collection.JavaConverters._
import uk.ac.warwick.tabula.ItemNotFoundException

@Entity
@DiscriminatorValue(value="ModeratedMarking")
class ModeratedMarkingWorkflow extends MarkingWorkflow with NoThirdMarker with AssignmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = ModeratedMarking

	override def onlineMarkingUrl(assignment: Assignment, marker: User, studentId: String) = {
		assignment.submissions.asScala.find(_.userId == studentId) match {
			case None => throw new ItemNotFoundException
			case Some(submission) =>
				if (submission.isReleasedToSecondMarker && getStudentsSecondMarker(assignment, submission.universityId).exists(_ == marker.getUserId))
					Routes.coursework.admin.assignment.onlineModeration(assignment, marker)
				else
					Routes.coursework.admin.assignment.markerFeedback.onlineFeedback(assignment, marker)
		}

	}

	// True if this marking workflow uses a second marker
	def hasSecondMarker = true
	def secondMarkerRoleName = Some("Moderator")
	def secondMarkerVerb = Some("moderate")

	override def getStudentsPrimaryMarker(assignment: Assignment, universityId: String) = getStudentsSecondMarker(assignment, universityId)
}
