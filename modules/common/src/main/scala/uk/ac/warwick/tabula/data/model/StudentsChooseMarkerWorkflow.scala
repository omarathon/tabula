package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.MarkingMethod.StudentsChooseMarker
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.Routes

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

	def onlineMarkingUrl(assignment: Assignment, marker: User) = Routes.onlineMarkerFeedback(assignment)

	def getStudentsFirstMarker(assignment: Assignment, universityId: String): Option[String] =
		assignment.markerSelectField.flatMap { field =>
			val submission = submissionService.getSubmissionByUniId(assignment, universityId)
			submission.flatMap(_.getValue(field).map(_.value))
		}


	def getStudentsSecondMarker(assignment: Assignment, universityId: String) = None

	def getSubmissions(assignment: Assignment, user: User) = assignment.markerSelectField.map { markerField =>
		val releasedSubmission = assignment.submissions.asScala.filter(_.isReleasedForMarking)
		releasedSubmission.filter(submission => {
			submission.getValue(markerField) match {
				case Some(subValue) => user.getUserId == subValue.value
				case None => false
			}
		})
	}.getOrElse(Nil)

}