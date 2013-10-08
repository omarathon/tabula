package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarking

@Entity
@DiscriminatorValue(value="SeenSecondMarking")
class SeenSecondMarkingWorkflow extends MarkingWorkflow {

	def this(dept: Department) = {
		this()
		this.department = dept
	}

	def markingMethod = SeenSecondMarking

	def hasSecondMarker = true

	private def getMarkersFromAssignmentMap(assignment: Assignment, universityId: String, markers: UserGroup) = {
		val student = userLookup.getUserByWarwickUniId(universityId)
		val mapEntry = Option(assignment.markerMap) flatMap {_.find{p:(String,UserGroup) =>
			p._2.includes(student.getUserId) && markers.includes(p._1)
		}}
		mapEntry match {
			case Some((markerId, students)) => Some(markerId)
			case _ => None
		}
	}

	def getStudentsFirstMarker(assignment: Assignment, universityId: String) =
		getMarkersFromAssignmentMap(assignment, universityId, assignment.markingWorkflow.firstMarkers)

	def getStudentsSecondMarker(assignment: Assignment, universityId: String) =
		getMarkersFromAssignmentMap(assignment, universityId, assignment.markingWorkflow.secondMarkers)


	def getSubmissions(assignment: Assignment, user: User) = {
		val isFirstMarker = assignment.isFirstMarker(user)
		val isSecondMarker = assignment.isSecondMarker(user)
		val studentUg = Option(assignment.markerMap.get(user.getUserId))
		studentUg match {
			case Some(ug) => {
				val submissionIds = ug.includeUsers
				if(isFirstMarker)
					assignment.submissions.filter(s => submissionIds.exists(_ == s.userId) && s.isReleasedForMarking)
				else if(isSecondMarker)
					assignment.submissions.filter(s => submissionIds.exists(_ == s.userId) && s.isReleasedToSecondMarker)
				else
					Seq()
			}
			case None => Seq()
		}
	}
}
