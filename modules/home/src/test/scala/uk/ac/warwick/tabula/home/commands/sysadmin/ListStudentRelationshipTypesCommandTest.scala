package uk.ac.warwick.tabula.home.commands.sysadmin

import uk.ac.warwick.tabula.Mockito

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class ListStudentRelationshipTypesCommandTest extends TestBase with Mockito {
	
	private trait Fixture {
		val commandInternal = new ListStudentRelationshipTypesCommandInternal with RelationshipServiceComponent {
			var relationshipService = mock[RelationshipService]
		}
	}
	
	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = ListStudentRelationshipTypesCommand()

			command.isInstanceOf[Appliable[Seq[StudentRelationshipType]]] should be(true)
		}
	}
	
	@Test
	def commandListsRelationshipTypesWhenApplied() {
		new Fixture {
			val tutorRT = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
			val supervisorRT = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")
			
			val allTypes = Seq(tutorRT, supervisorRT)
			
			commandInternal.relationshipService.allStudentRelationshipTypes returns (allTypes)

			commandInternal.applyInternal() should be (allTypes)

			there was one(commandInternal.relationshipService).allStudentRelationshipTypes
		}
	}
	
	@Test
	def permissionsRequireGlobalStudentRelationshipTypeRead {
		new Fixture {
			val perms = new ListStudentRelationshipTypesCommandPermissions() {}
			val checking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			there was one(checking).PermissionCheck(Permissions.StudentRelationshipType.Read)
		}
	}

}