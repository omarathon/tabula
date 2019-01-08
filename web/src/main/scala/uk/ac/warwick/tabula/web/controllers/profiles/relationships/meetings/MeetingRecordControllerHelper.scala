package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType}

trait MeetingRecordControllerHelper {
	def chosenRelationships(
		relationshipType: StudentRelationshipType,
		manageableRelationships: Seq[StudentRelationship],
		currentUser: CurrentUser
	): Seq[StudentRelationship] = {

		implicit class FilterOrAll[A](s: Seq[A]) {
			// filters a collection on the specified predicate but returns the original collection if that leaves no elements
			def filterOrAll(p: A => Boolean): Seq[A] = {
				val result = s.filter(p)
				if(result.isEmpty) s else result
			}
		}

		val isStudent = manageableRelationships.exists(_.studentCourseDetails.student.universityId == currentUser.universityId)

		if(isStudent) {
			manageableRelationships.filterOrAll(_.relationshipType == relationshipType).headOption.toSeq
		} else {
			manageableRelationships
				.filterOrAll(rel => rel.agentMember.map(_.universityId).contains(currentUser.universityId))
				.filterOrAll(_.relationshipType == relationshipType)
		}
	}
}
