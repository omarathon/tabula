package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.{RelationshipServiceComponent, RelationshipService, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions


class ViewRelatedStudentsCommandTest extends TestBase with Mockito {

	val member = new StaffMember("test")

	@Test
	def listsAllStudentsWithTutorRelationship(){

		val mockRelationshipService = mock[RelationshipService]
		val rels = Seq(new StudentRelationship)
		val relationshipType = new StudentRelationshipType
		
		mockRelationshipService.listStudentRelationshipsWithMember(relationshipType, member) returns rels

		val command = new ViewRelatedStudentsCommandInternal(member, relationshipType) with RelationshipServiceComponent {
			var relationshipService = mockRelationshipService
		}

		command.applyInternal() should be (rels)
	}

	@Test
	def listsAllStudentsWithSupervisorRelationship(){

		val mockRelationshipService = mock[RelationshipService]
		val rels = Seq(new StudentRelationship)
		val relationshipType = new StudentRelationshipType
		
		mockRelationshipService.listStudentRelationshipsWithMember(relationshipType, member) returns rels

		val command = new ViewRelatedStudentsCommandInternal(member, relationshipType) with RelationshipServiceComponent {
			var relationshipService = mockRelationshipService
		}

		command.applyInternal() should be(rels)
	}

}
