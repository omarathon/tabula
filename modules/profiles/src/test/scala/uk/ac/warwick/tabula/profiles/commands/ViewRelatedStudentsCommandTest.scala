package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.{RelationshipServiceComponent, RelationshipService, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions


class ViewRelatedStudentsCommandTest extends TestBase with Mockito {

	val member = new StaffMember("test")

	@Test
	def listsAllStudentsWithTutorRelationship(){

		val mockRelationshipService = mock[RelationshipService]
    val rels = Seq(new StudentRelationship)
		mockRelationshipService.listStudentRelationshipsWithMember(PersonalTutor, member) returns rels

		val command = new ViewRelatedStudentsCommandInternal(member,PersonalTutor) with RelationshipServiceComponent   {
				var relationshipService = mockRelationshipService
		}

		command.applyInternal() should be(rels)
	}

	@Test
	def listsAllStudentsWithSupervisorRelationship(){

		val mockRelationshipService = mock[RelationshipService]
		val rels = Seq(new StudentRelationship)
		mockRelationshipService.listStudentRelationshipsWithMember(Supervisor, member) returns rels

		val command = new ViewRelatedStudentsCommandInternal(member,Supervisor) with RelationshipServiceComponent   {
			var relationshipService = mockRelationshipService
		}

		command.applyInternal() should be(rels)
	}

	@Test
	def requiresReadTuteesPermissionForTutorRels(){
		val command = new ViewRelatedStudentsCommandInternal(member, PersonalTutor) with RelationshipServiceComponent   {
			var relationshipService = mock[RelationshipService]
		}
		val permissionsChecking = mock[PermissionsChecking]
		command.permissionsCheck(permissionsChecking)

		there was one(permissionsChecking).PermissionCheck(Permissions.Profiles.Read.PersonalTutees, member)
	}

	@Test
	def requiresReadSuperviseesPermissionForSupervisorRels(){
		val command = new ViewRelatedStudentsCommandInternal(member, Supervisor) with RelationshipServiceComponent   {
			var relationshipService = mock[RelationshipService]
		}
		val permissionsChecking = mock[PermissionsChecking]

		command.permissionsCheck(permissionsChecking)

		there was one(permissionsChecking).PermissionCheck(Permissions.Profiles.Read.Supervisees, member)
	}

}
