package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.MarkingMethod.StudentsChooseMarker
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.spring.Wire

@Entity
@DiscriminatorValue(value="StudentsChooseMarker")
class StudentsChooseMarkerWorkflow extends MarkingWorkflow with NoSecondMarker {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	@transient var submissionService = Wire[SubmissionService]

	def markingMethod = StudentsChooseMarker

	override def studentsChooseMarker = true

	def onlineMarkingUrl(assignment: Assignment, marker: User) = MarkingRoutes.onlineMarkerFeedback(assignment)

	def getStudentsFirstMarker(assignment: Assignment, universityId: String) = assignment.markerSelectField match {
		case Some(field) => {
			val submission = submissionService.getSubmissionByUniId(assignment, universityId)
			submission.flatMap(_.getValue(field).map(_.value))
		}
		case _ => None
	}

	def getStudentsSecondMarker(assignment: Assignment, universityId: String) = None

	def getSubmissions(assignment: Assignment, user: User) = assignment.markerSelectField match {
		case Some(markerField) => {
			val releasedSubmission = assignment.submissions.asScala.filter(_.isReleasedForMarking)
			releasedSubmission.filter(submission => {
				submission.getValue(markerField) match {
					case Some(subValue) => user.getUserId == subValue.value
					case None => false
				}
			})
		}
		case None => Seq()
	}
}