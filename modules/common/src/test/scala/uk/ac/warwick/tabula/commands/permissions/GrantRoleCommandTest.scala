package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, DepartmentGrantedRole, GrantedRole}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{UniversityMemberRoleDefinition, BuiltInRoleDefinition, DepartmentalAdministratorRoleDefinition}
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.permissions.Permissions.{ReplicaSyncing, ImportSystemData}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.SecurityService

class GrantRoleCommandTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]
	val securityService = mock[SecurityService]

	// a role with a single permission to keep things simple
	val singlePermissionsRoleDefinition = new BuiltInRoleDefinition(){
		override def description="test"
		GrantsScopedPermission(
			Permissions.Department.ArrangeModules)
		def canDelegateThisRolesPermissions:JBoolean = false
	}

	private def command[A <: PermissionsTarget: ClassTag](scope: A) = {
		val cmd = new GrantRoleCommand(scope)
		cmd.permissionsService = permissionsService
		cmd.securityService = securityService
		cmd
	}
	
	@Test def itWorksForNewRole {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (None)
				
		val grantedPerm = cmd.applyInternal()
		grantedPerm.roleDefinition should be (DepartmentalAdministratorRoleDefinition)
		grantedPerm.users.includeUsers.size() should be (2)
		grantedPerm.users.includes("cuscav") should be (true)
		grantedPerm.users.includes("cusebr") should be (true)
		grantedPerm.users.includes("cuscao") should be (false)
		grantedPerm.scope should be (dept)
	}
	
	@Test def itWorksWithExisting {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		val existing = GrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		existing.users.addUser("cuscao")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
				
		val grantedPerm = cmd.applyInternal()
		(grantedPerm.eq(existing)) should be (true)
		
		grantedPerm.roleDefinition should be (DepartmentalAdministratorRoleDefinition)
		grantedPerm.users.includeUsers.size() should be (3)
		grantedPerm.users.includes("cuscav") should be (true)
		grantedPerm.users.includes("cusebr") should be (true)
		grantedPerm.users.includes("cuscao") should be (true)
		grantedPerm.scope should be (dept)
	}
	
	@Test def validatePasses { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")

		val cmd = command(dept)
		cmd.roleDefinition = singlePermissionsRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")

		permissionsService.getGrantedRole(dept, singlePermissionsRoleDefinition) returns (None)
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, dept) returns true


		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (false)
	}}
	
	@Test def noUsercodes { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = singlePermissionsRoleDefinition
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (None)
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, dept) returns true

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}
	
	@Test def duplicateUsercode { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = singlePermissionsRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.usercodes.add("cuscao")
		
		val existing = GrantedRole(dept, singlePermissionsRoleDefinition)
		existing.users.addUser("cuscao")
		
		permissionsService.getGrantedRole(dept, singlePermissionsRoleDefinition) returns (Some(existing))
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, dept) returns true

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("userId.duplicate")
	}}
	
	@Test def noPermission { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		permissionsService.getGrantedRole(dept, null) returns (None)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("roleDefinition")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}
	
	@Test def cantGiveWhatYouDontHave { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (None)
		securityService.canDelegate(currentUser,Permissions.Department.ArrangeModules, dept) returns false

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		(errors.getErrorCount >= 1) should be (true)
		errors.getFieldError.getField should be ("roleDefinition")
		errors.getFieldError.getCode should be ("permissions.cantGiveWhatYouDontHave")
	}}

}