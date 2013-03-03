package uk.ac.warwick.tabula.home.commands.sysadmin

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.services.permissions.RoleService
import org.springframework.core.convert.ConversionService
import uk.ac.warwick.userlookup.User
import org.mockito.Matchers._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.PermissionDefinition
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.RoleBuilder.GeneratedRole
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.Fixtures
import org.springframework.validation.BindException

class PermissionsHelperCommandTest extends TestBase with Mockito {
	
	val securityService = mock[SecurityService]
	val roleService = mock[RoleService]
	val conversionService = mock[ConversionService]
	
	private def newCommand() = {
		val cmd = new PermissionsHelperCommand
		cmd.securityService = securityService
		cmd.roleService = roleService
		cmd.conversionService = conversionService
		cmd
	}
	
	@Test def justUser {
		val user = new User("cuscav") 
		
		val cmd = newCommand()
		cmd.user = user
		
		val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
		val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)
		
		roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			pd1, pd2
		))
		
		val r1 = new GeneratedRole(None, "r1")
		val r2 = new GeneratedRole(None, "r2")
		
		roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			r1, r2
		))
		
		val result = cmd.applyInternal
		there was no (securityService).can(isA[CurrentUser], isA[Permission], isA[PermissionsTarget])
		
		result.canDo should be (false)
		result.permissions should be (Seq(pd1, pd2))
		result.roles should be (Seq(r1, r2))
		result.resolvedScope should be (null)
		result.scopeMismatch should be (false)
		result.scopeMissing should be (true)
	}
	
	@Test def scopeMismatch {
		val user = new User("cuscav") 
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Archive
		
		val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
		val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)
		
		roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			pd1, pd2
		))
		
		val r1 = new GeneratedRole(None, "r1")
		val r2 = new GeneratedRole(None, "r2")
		
		roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			r1, r2
		))
		
		securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isNull[PermissionsTarget]) returns (false)
		
		val result = cmd.applyInternal
		
		result.canDo should be (false)
		result.permissions should be (Seq(pd1, pd2))
		result.roles should be (Seq(r1, r2))
		result.resolvedScope should be (null)
		result.scopeMismatch should be (true)
		result.scopeMissing should be (true)
	}
	
	
	
	@Test def scopeMismatchWithScopeless {
		val user = new User("cuscav") 
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.UserPicker
		
		val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
		val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)
		
		roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			pd1, pd2
		))
		
		val r1 = new GeneratedRole(None, "r1")
		val r2 = new GeneratedRole(None, "r2")
		
		roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			r1, r2
		))
		
		securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isNull[PermissionsTarget]) returns (false)
		
		val result = cmd.applyInternal
		
		result.canDo should be (false)
		result.permissions should be (Seq(pd1, pd2))
		result.roles should be (Seq(r1, r2))
		result.resolvedScope should be (null)
		result.scopeMismatch should be (false)
		result.scopeMissing should be (true)
	}
	
	@Test def scopeResolved {
		val user = new User("cuscav") 
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Archive
		cmd.scopeType = classOf[Department]
		cmd.scope = "in"
			
		val dept = Fixtures.department("in")
		conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
		conversionService.convert("in", classOf[Department]) returns (dept)
		
		val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
		val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)
		
		roleService.getExplicitPermissionsFor(isA[CurrentUser], isEq(dept)) returns (Stream(
			pd1, pd2
		))
		
		val r1 = new GeneratedRole(None, "r1")
		val r2 = new GeneratedRole(None, "r2")
		
		roleService.getRolesFor(isA[CurrentUser], isEq(dept)) returns (Stream(
			r1, r2
		))
		
		securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isEq(dept)) returns (true)
		
		val result = cmd.applyInternal
		
		result.canDo should be (true)
		result.permissions should be (Seq(pd1, pd2))
		result.roles should be (Seq(r1, r2))
		result.resolvedScope should be (dept)
		result.scopeMismatch should be (false)
		result.scopeMissing should be (false)
	}
	
	@Test def cantResolveScope {
		val user = new User("cuscav") 
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Archive
		cmd.scopeType = classOf[Department]
		cmd.scope = "in"
			
		conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
		conversionService.convert("in", classOf[Department]) returns (null)
		
		val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
		val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)
		
		roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			pd1, pd2
		))
		
		val r1 = new GeneratedRole(None, "r1")
		val r2 = new GeneratedRole(None, "r2")
		
		roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (Stream(
			r1, r2
		))
		
		securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isNull[PermissionsTarget]) returns (false)
		
		val result = cmd.applyInternal
		
		result.canDo should be (false)
		result.permissions should be (Seq(pd1, pd2))
		result.roles should be (Seq(r1, r2))
		result.resolvedScope should be (null)
		result.scopeMismatch should be (true)
		result.scopeMissing should be (true)
	}
	
	@Test def validatePasses {
		val user = new User("cuscav") 
		user.setFoundUser(true)
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Create
		cmd.scopeType = classOf[Department]
		cmd.scope = "in"
			
		val dept = Fixtures.department("in")
		conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
		conversionService.convert("in", classOf[Department]) returns (dept)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.hasErrors should be (false)
	}
	
	@Test def validateNoPermissionOrScopePasses {
		val user = new User("cuscav") 
		user.setFoundUser(true)
		
		val cmd = newCommand()
		cmd.user = user
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.hasErrors should be (false)
	}
	
	@Test def validateNoUser {
		val cmd = newCommand()
		cmd.permission = Permissions.Assignment.Create
		cmd.scopeType = classOf[Department]
		cmd.scope = "in"
			
		val dept = Fixtures.department("in")
		conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
		conversionService.convert("in", classOf[Department]) returns (dept)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("user")
		errors.getFieldError.getCode should be ("permissionsHelper.user.invalid")
	}
	
	@Test def validateNotFoundUser {
		val user = new User("cuscav") 
		user.setFoundUser(false)
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Create
		cmd.scopeType = classOf[Department]
		cmd.scope = "in"
			
		val dept = Fixtures.department("in")
		conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
		conversionService.convert("in", classOf[Department]) returns (dept)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("user")
		errors.getFieldError.getCode should be ("permissionsHelper.user.invalid")
	}
	
	@Test def validateInvalidScopeType {
		val user = new User("cuscav") 
		user.setFoundUser(true)
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Create
		cmd.scopeType = classOf[PermissionsTarget]
		cmd.scope = "in"
			
		conversionService.canConvert(classOf[String], classOf[PermissionsTarget]) returns (false)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("scopeType")
		errors.getFieldError.getCode should be ("permissionsHelper.scopeType.invalid")
	}
	
	@Test def validateInvalidScope {
		val user = new User("cuscav") 
		user.setFoundUser(true)
		
		val cmd = newCommand()
		cmd.user = user
		cmd.permission = Permissions.Assignment.Create
		cmd.scopeType = classOf[Department]
		cmd.scope = "in"
			
		val dept = Fixtures.department("in")
		conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
		conversionService.convert("in", classOf[Department]) returns (null)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("scope")
		errors.getFieldError.getCode should be ("permissionsHelper.scope.invalid")
	}

}