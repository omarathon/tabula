package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.data.model.{StudentRelationship, Member}

case class StudentRelationshipChange(
	oldAgents: Seq[Member],
	modifiedRelationship: StudentRelationship
)
