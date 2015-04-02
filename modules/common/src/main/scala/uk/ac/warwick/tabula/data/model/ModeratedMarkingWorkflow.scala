package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.MarkingMethod.ModeratedMarking
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.Routes

@Entity
@DiscriminatorValue(value="ModeratedMarking")
class ModeratedMarkingWorkflow extends MarkingWorkflow with NoThirdMarker with AssessmentMarkerMap {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = ModeratedMarking

	override def courseworkMarkingUrl(assignment: Assignment, marker: User, studentId: String) = {
		if (assignment.isReleasedToSecondMarker(studentId) && getStudentsSecondMarker(assignment, studentId).contains(marker.getUserId))
			Routes.coursework.admin.assignment.onlineModeration(assignment, marker)
		else
			Routes.coursework.admin.assignment.markerFeedback.onlineFeedback(assignment, marker)
	}

	override def examMarkingUrl(exam: Exam, marker: User, studentId: String) = {
		if (exam.isReleasedToSecondMarker(studentId) && getStudentsSecondMarker(exam, studentId).contains(marker.getUserId))
			Routes.exams.admin.onlineModeration(exam, marker)
		else
			Routes.exams.admin.markerFeedback.onlineFeedback(exam, marker)
	}

	// True if this marking workflow uses a second marker
	def hasSecondMarker = true
	def secondMarkerRoleName = Some("Moderator")
	def secondMarkerVerb = Some("moderate")

	override def getStudentsPrimaryMarker(assessment: Assessment, universityId: String) = getStudentsSecondMarker(assessment, universityId)
}
