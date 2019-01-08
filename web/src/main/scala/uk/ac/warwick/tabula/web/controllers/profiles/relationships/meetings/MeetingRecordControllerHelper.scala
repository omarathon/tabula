package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType}

trait MeetingRecordControllerHelper {
	def chosenRelationships(
		relationshipType: StudentRelationshipType,
		manageableRelationships: Seq[StudentRelationship],
		currentUser: CurrentUser
	): Seq[StudentRelationship] = {
		// Go through the relationships for this SPR code and find one where the current user is the agent.
		// and also the correct relationType
		val filteredRelationships = manageableRelationships
			.filter(rel => rel.agentMember.map(_.universityId).contains(currentUser.universityId))
			.filter(_.relationshipType == relationshipType)

		if(filteredRelationships.isEmpty) manageableRelationships else filteredRelationships
	}
}
