package uk.ac.warwick.tabula.home.commands.sysadmin

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.data.model.StudentRelationshipSource
import org.springframework.validation.BindException

class AddStudentRelationshipTypeCommandTest extends TestBase with Mockito {
	
	private trait Fixture {
		val commandInternal = new AddStudentRelationshipTypeCommandInternal with RelationshipServiceComponent {
			var relationshipService = mock[RelationshipService]
		}
	}
	
	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = AddStudentRelationshipTypeCommand()

			command.isInstanceOf[Appliable[StudentRelationshipType]] should be(true)
		}
	}
	
	@Test
	def commandCreatesRelationshipWhenApplied() {
		new Fixture {
			commandInternal.id = "theId"
			commandInternal.urlPart = "theUrlPart"
			commandInternal.description = "role description"		
			commandInternal.agentRole = "agent"
			commandInternal.studentRole = "student"
			commandInternal.defaultSource = StudentRelationshipSource.SITS
			commandInternal.defaultDisplay = false
			commandInternal.expectedUG = true
			commandInternal.expectedPGT = true
			commandInternal.expectedPGR = true
			commandInternal.sortOrder = 1234

			val newType = commandInternal.applyInternal()

			newType.id should be ("theId")
			newType.urlPart should be ("theUrlPart")
			newType.description should be ("role description")		
			newType.agentRole should be ("agent")
			newType.studentRole should be ("student")
			newType.defaultSource should be (StudentRelationshipSource.SITS)
			newType.defaultDisplay.booleanValue should be (false)
			newType.expectedUG.booleanValue should be (true)
			newType.expectedPGT.booleanValue should be (true)
			newType.expectedPGR.booleanValue should be (true)
			newType.sortOrder should be (1234)
		}
	}

	@Test
	def commandApplyInvokesSaveOnRelationshipService() {
		new Fixture {
			val newType = commandInternal.applyInternal()
			there was one(commandInternal.relationshipService).saveOrUpdate(newType)
		}
	}

	@Test
	def commandDescriptionDescribedProperties() {
		new Fixture {
			val describable = new ModifyStudentRelationshipTypeCommandDescription with StudentRelationshipTypeProperties {
				val eventName: String = "test"
			}
			
			describable.id = "theId"
			describable.urlPart = "theUrlPart"
			describable.description = "role description"

			val description = mock[Description]
			describable.describe(description)
			there was one(description).properties(
				"id" -> "theId",
				"urlPart" -> "theUrlPart",
				"description" -> "role description"
			)
		}
	}
	
	@Test
	def permissionsRequireGlobalStudentRelationshipTypeCreate {
		new Fixture {
			val perms = new AddStudentRelationshipTypeCommandPermissions() {}
			val checking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			there was one(checking).PermissionCheck(Permissions.StudentRelationshipType.Create)
		}
	}
	
	@Test
	def duplicateValidation {
		new Fixture {
			commandInternal.id = "newId"
				
			val existing = StudentRelationshipType("existing", "existing", "existing", "existing")
				
			commandInternal.relationshipService.getStudentRelationshipTypeByUrlPart("url") returns (None)
			commandInternal.relationshipService.getStudentRelationshipTypeByUrlPart("existing") returns (Some(existing))

			commandInternal.urlPart = "existing"
			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)

			commandInternal.urlPart = "url"
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}

}