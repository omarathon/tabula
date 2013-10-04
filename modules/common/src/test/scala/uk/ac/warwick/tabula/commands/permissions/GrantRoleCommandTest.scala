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

class GrantRoleCommandTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]

	private def command[A <: PermissionsTarget: ClassTag](scope: A) = {
		val cmd = new GrantRoleCommand(scope)
		cmd.permissionsService = permissionsService

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
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (None)
		val deptAdminWithGrantOption = new CustomRoleDefinition().tap(crd=>{
			crd.baseRoleDefinition = DepartmentalAdministratorRoleDefinition
			crd.canDelegateThisRolesPermissions = true
		})
		permissionsService.getAllGrantedRolesFor(currentUser) returns Seq(new DepartmentGrantedRole(dept, deptAdminWithGrantOption))

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (false)
	}}
	
	@Test def noUsercodes { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (None)
		val deptAdminWithGrantOption = new CustomRoleDefinition().tap(crd=>{
			crd.baseRoleDefinition = DepartmentalAdministratorRoleDefinition
			crd.canDelegateThisRolesPermissions = true
		})
		permissionsService.getAllGrantedRolesFor(currentUser) returns Seq(new DepartmentGrantedRole(dept, deptAdminWithGrantOption))

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
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.usercodes.add("cuscao")
		
		val existing = GrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		existing.users.addUser("cuscao")
		
		permissionsService.getGrantedRole(dept, DepartmentalAdministratorRoleDefinition) returns (Some(existing))
		val deptAdminWithGrantOption = new CustomRoleDefinition().tap(crd=>{
			crd.baseRoleDefinition = DepartmentalAdministratorRoleDefinition
			crd.canDelegateThisRolesPermissions = true
		})
		permissionsService.getAllGrantedRolesFor(currentUser) returns Seq(new DepartmentGrantedRole(dept, deptAdminWithGrantOption))
		
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
		val deptAdminWithoutGrantOption = new CustomRoleDefinition().tap(crd=>{
			crd.baseRoleDefinition = DepartmentalAdministratorRoleDefinition
			crd.canDelegateThisRolesPermissions = false
		})
		permissionsService.getAllGrantedRolesFor(currentUser) returns Seq(new DepartmentGrantedRole(dept, deptAdminWithoutGrantOption))
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		(errors.getErrorCount >= 1) should be (true)
		errors.getFieldError.getField should be ("roleDefinition")
		errors.getFieldError.getCode should be ("permissions.cantGiveWhatYouDontHave")
	}}


	object TestRoleDef extends BuiltInRoleDefinition{
		override def description="test"
		GrantsScopedPermission(
			Permissions.Module.ManageAssignments,
			Permissions.Department.ArrangeModules)
		  def canDelegateThisRolesPermissions:JBoolean = false

	}

	object TestScopelessRoleDef extends BuiltInRoleDefinition{
		override def description ="test"
		GrantsScopelessPermission(ImportSystemData)
		GrantsScopelessPermission(ReplicaSyncing)
		def canDelegateThisRolesPermissions:JBoolean = false

	}
	val testScope:PermissionsTarget = new PermissionsTarget {
		def permissionsParents: Stream[PermissionsTarget] = Stream.empty
		def id: String = "test"
	}

	val testChildScope = new PermissionsTarget {
		def permissionsParents: Stream[PermissionsTarget] =testScope #:: Stream.empty
		def id: String = "testChild"
	}

	val unrelatedScope:PermissionsTarget = new PermissionsTarget {
		def permissionsParents: Stream[PermissionsTarget] = Stream.empty
		def id: String = "fribble"
	}

	type DelegatePerms = Map[Permission,Seq[Option[PermissionsTarget]]]
	@Test
	def getDeniedPermissionsWithASinglePermission(){
		val delegatablePermissions:DelegatePerms = Map(Permissions.Department.ArrangeModules->Seq(Some(testScope)))
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestRoleDef,testScope).head should be(Permissions.Module.ManageAssignments)
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestRoleDef,testScope).tail should be(Nil)
	}

	@Test
	def getDeniedPermissionWithParentScope(){
		// if I can delegate on the parent, then I can delegate on the child
		val delegatablePermissions:DelegatePerms = Map(Permissions.Department.ArrangeModules->Seq(Some(testScope)))
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestRoleDef,testChildScope).head should be(Permissions.Module.ManageAssignments)
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestRoleDef,testChildScope).tail should be(Nil)
	}

	@Test
	def getDeniedPermissionWithChildScope(){
		// Being able to delegate permissions on a child scope doesn't mean I can delegate on its parent
		val delegatablePermissions:DelegatePerms = Map(Permissions.Department.ArrangeModules->Seq(Some(testChildScope)))
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestRoleDef,testScope).toSeq.contains(Permissions.Department.ArrangeModules) should be(true)
	}

	@Test
	def getDeniedPermissionAllowsScopelessPermissions(){
		// A scopeless permission should be permitted it the delgatable permissions list contains it, regardless of whether scopes match
		val delegatablePermissions:DelegatePerms = Map(ImportSystemData->Seq(None))
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestScopelessRoleDef,testScope).head should be(ReplicaSyncing)
		GrantRoleCommand.getDeniedPermissions(delegatablePermissions,TestScopelessRoleDef,testScope).tail should be(Nil)
	}

	@Test
	def canGrantUniversityMember(){
		// university member is a good role to test with as it has a mix of scoped and scopeless permissions
		// the test is a bit contrived though as we first create a role with universityMember over a whole department
		// use that granting role to grant UniversityMember to a user - normally this would never happen
		// as UniversityMemberRole can only be created with a Member as it's scope.

		val uniMemberGrantingRole = new CustomRoleDefinition().tap(d=>{
			d.canDelegateThisRolesPermissions = true
			d.builtInBaseRoleDefinition = UniversityMemberRoleDefinition
		})
		val uniMemberDelegatablePermissions = uniMemberGrantingRole.delegatablePermissions(Some(testScope)).mapValues(Seq(_))
		GrantRoleCommand.getDeniedPermissions(uniMemberDelegatablePermissions, UniversityMemberRoleDefinition,testChildScope) should be (Nil)
	}

}