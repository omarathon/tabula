package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.permissions.{PermissionsServiceComponent, PermissionsService}
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, DepartmentalAdministratorRoleDefinition}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, SecurityService}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.{Describable, SelfValidating, Appliable, DescriptionImpl}

class GrantRoleCommandTest extends TestBase with Mockito {

	trait CommandTestSupport[A <: PermissionsTarget] extends GrantRoleCommandState[A] with PermissionsServiceComponent with SecurityServiceComponent {
		val permissionsService = mock[PermissionsService]
		val securityService = mock[SecurityService]
	}

	// a role with a single permission to keep things simple
	val singlePermissionsRoleDefinition = new BuiltInRoleDefinition(){
		override val getName="test"
		override def description="test"
		GrantsScopedPermission(
			Permissions.Department.ArrangeModules)
		def canDelegateThisRolesPermissions:JBoolean = false
	}

	trait Fixture {
		val department = Fixtures.department("in", "IT Services")

		val command = new GrantRoleCommandInternal(department) with CommandTestSupport[Department] with GrantRoleCommandValidation
	}
	
	@Test def itWorksForNewRole { new Fixture {
		command.roleDefinition = DepartmentalAdministratorRoleDefinition
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns (None)
				
		val grantedPerm = command.applyInternal()
		grantedPerm.roleDefinition should be (DepartmentalAdministratorRoleDefinition)
		grantedPerm.users.size should be (2)
		grantedPerm.users.knownType.includesUserId("cuscav") should be (true)
		grantedPerm.users.knownType.includesUserId("cusebr") should be (true)
		grantedPerm.users.knownType.includesUserId("cuscao") should be (false)
		grantedPerm.scope should be (department)
	}}
	
	@Test def itWorksWithExisting { new Fixture {
		command.roleDefinition = DepartmentalAdministratorRoleDefinition
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		
		val existing = GrantedRole(department, DepartmentalAdministratorRoleDefinition)
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
				
		val grantedPerm = command.applyInternal()
		(grantedPerm.eq(existing)) should be (true)
		
		grantedPerm.roleDefinition should be (DepartmentalAdministratorRoleDefinition)
		grantedPerm.users.size should be (3)
		grantedPerm.users.knownType.includesUserId("cuscav") should be (true)
		grantedPerm.users.knownType.includesUserId("cusebr") should be (true)
		grantedPerm.users.knownType.includesUserId("cuscao") should be (true)
		grantedPerm.scope should be (department)
	}}
	
	@Test def validatePasses { withUser("cuscav", "0672089") { new Fixture {
		command.roleDefinition = singlePermissionsRoleDefinition
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (None)
		command.securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, department) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}}
	
	@Test def noUsercodes { withUser("cuscav", "0672089") { new Fixture {
		command.roleDefinition = singlePermissionsRoleDefinition

		command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns (None)
		command.securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, department) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}
	
	@Test def duplicateUsercode { withUser("cuscav", "0672089") { new Fixture {
		command.roleDefinition = singlePermissionsRoleDefinition
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.usercodes.add("cuscao")
		
		val existing = GrantedRole(department, singlePermissionsRoleDefinition)
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))
		command.securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, department) returns true

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("userId.duplicate")
	}}}
	
	@Test def noPermission { withUser("cuscav", "0672089") { new Fixture {
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		command.permissionsService.getGrantedRole(department, null) returns (None)
		
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("roleDefinition")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}
	
	@Test def cantGiveWhatYouDontHave { withUser("cuscav", "0672089") { new Fixture {
		command.roleDefinition = DepartmentalAdministratorRoleDefinition
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns (None)
		command.securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, department) returns false

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		(errors.getErrorCount >= 1) should be (true)
		errors.getFieldError.getField should be ("roleDefinition")
		errors.getFieldError.getCode should be ("permissions.cantGiveWhatYouDontHave")
	}}}

	@Test
	def describe {
		val dept = Fixtures.department("in")
		dept.id = "dept-id"

		val command = new GrantRoleCommandDescription[Department] with CommandTestSupport[Department] {
			val eventName: String = "test"

			val scope = dept
			val grantedRole = None
		}

		command.roleDefinition = singlePermissionsRoleDefinition
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"scope" -> "Department[dept-id]",
			"usercodes" -> "cuscav,cusebr",
			"roleDefinition" -> "test"
		))
	}

	@Test def gluesEverythingTogether {
		val department = Fixtures.department("in")
		val command = GrantRoleCommand(department)

		command should be (anInstanceOf[Appliable[GrantedRole[Department]]])
		command should be (anInstanceOf[GrantRoleCommandState[Department]])
		command should be (anInstanceOf[GrantRoleCommandPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[GrantRoleCommandValidation])
		command should be (anInstanceOf[Describable[GrantedRole[Department]]])
	}

}