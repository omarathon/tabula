package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import org.mockito.Matchers._
import uk.ac.warwick.tabula.permissions.Permission

class RevokeRoleCommandTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]
	
	private def command[A <: PermissionsTarget : Manifest](scope: A) = {
		val cmd = new RevokeRoleCommand(scope)
		cmd.permissionsService = permissionsService
		
		cmd
	}
	
	@Test def nonExistingRole {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (None)
				
		// Doesn't blow up, just a no-op
		cmd.applyInternal() should be (null)
	}
	
	@Test def itWorksWithExisting {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		val existing = GrantedRole.init(dept, DepartmentalAdministratorRoleDefinition)
		existing.users.addUser("cuscao")
		existing.users.addUser("cuscav")
		existing.users.addUser("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
				
		val grantedPerm = cmd.applyInternal()
		(grantedPerm.eq(existing)) should be (true)
		
		grantedPerm.roleDefinition should be (DepartmentalAdministratorRoleDefinition)
		grantedPerm.users.includeUsers.size() should be (1)
		grantedPerm.users.includes("cuscav") should be (false)
		grantedPerm.users.includes("cusebr") should be (false)
		grantedPerm.users.includes("cuscao") should be (true)
		grantedPerm.scope should be (dept)
	}
	
	@Test def validatePasses { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		val existing = GrantedRole.init(dept, DepartmentalAdministratorRoleDefinition)
		existing.users.addUser("cuscao")
		existing.users.addUser("cusebr")
		existing.users.addUser("cuscav")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (false)
	}}
	
	@Test def noUsercodes { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		
		val existing = GrantedRole.init(dept, DepartmentalAdministratorRoleDefinition)
		existing.users.addUser("cuscao")
		existing.users.addUser("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}
	
	@Test def usercodeNotInGroup { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cuscao")
		
		val existing = GrantedRole.init(dept, DepartmentalAdministratorRoleDefinition)
		existing.users.addUser("cuscao")
		existing.users.addUser("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("userId.notingroup")
	}}
	
	@Test def noRole { withUser("cuscav", "0672089") {
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

}