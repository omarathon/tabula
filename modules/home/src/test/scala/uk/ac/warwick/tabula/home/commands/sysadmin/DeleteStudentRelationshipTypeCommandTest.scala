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

class DeleteStudentRelationshipTypeCommandTest extends TestBase with Mockito {
	
	private trait Fixture {
		val testRelationshipType = new StudentRelationshipType 
		testRelationshipType.id = "trtId"
		testRelationshipType.urlPart = "trt-url"
		testRelationshipType.description = "trt role description"		
		testRelationshipType.agentRole = "trt agent"
		testRelationshipType.studentRole = "trt student"
		testRelationshipType.defaultSource = StudentRelationshipSource.Local
		testRelationshipType.defaultDisplay = true
		testRelationshipType.expectedUG = false
		testRelationshipType.expectedPGT = false
		testRelationshipType.expectedPGR = false
		testRelationshipType.sortOrder = 9
		
		val commandInternal = new DeleteStudentRelationshipTypeCommandInternal(testRelationshipType) with RelationshipServiceComponent {
			var relationshipService = mock[RelationshipService]
		}
	}
	
	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = DeleteStudentRelationshipTypeCommand(testRelationshipType)

			command.isInstanceOf[Appliable[StudentRelationshipType]] should be (true)
			command.isInstanceOf[HasExistingStudentRelationshipType] should be (true)
			command.asInstanceOf[HasExistingStudentRelationshipType].relationshipType should be (testRelationshipType)
		}
	}
	
	@Test
	def commandDeletesRelationshipWhenApplied() {
		new Fixture {
			commandInternal.applyInternal() should be (testRelationshipType)

			there was one(commandInternal.relationshipService).delete(testRelationshipType)
		}
	}

	@Test
	def commandDescriptionDescribedProperties() {
		new Fixture {
			val describable = new DeleteStudentRelationshipTypeCommandDescription with HasExistingStudentRelationshipType {
				val eventName: String = "test"
				val relationshipType = testRelationshipType
			}

			val description = mock[Description]
			describable.describe(description)
			there was one(description).properties(
				"id" -> "trtId",
				"urlPart" -> "trt-url",
				"description" -> "trt role description"
			)
		}
	}
	
	@Test
	def permissionsRequireGlobalStudentRelationshipTypeDelete {
		new Fixture {
			val perms = new DeleteStudentRelationshipTypeCommandPermissions with HasExistingStudentRelationshipType {
				val relationshipType = testRelationshipType
			}
			val checking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			there was one(checking).PermissionCheck(Permissions.StudentRelationshipType.Delete)
		}
	}
	
	@Test
	def emptyValidation {
		new Fixture {
			var relationshipService = mock[RelationshipService]
			relationshipService.countStudentsByRelationship(testRelationshipType) returns (5)
			testRelationshipType.relationshipService = relationshipService
			
			commandInternal.confirm = true

			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)
			
			relationshipService = mock[RelationshipService]
			relationshipService.countStudentsByRelationship(testRelationshipType) returns (0)
			testRelationshipType.relationshipService = relationshipService

			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}
	
	@Test
	def confirmValidation {
		new Fixture {
			var relationshipService = mock[RelationshipService]
			relationshipService.countStudentsByRelationship(testRelationshipType) returns (0)
			testRelationshipType.relationshipService = relationshipService

			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)
			
			commandInternal.confirm = true

			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}

}