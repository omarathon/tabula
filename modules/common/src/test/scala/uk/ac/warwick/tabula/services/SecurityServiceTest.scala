package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.hamcrest.Matchers._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.tabula.data.model.{Department, Module, StaffMember, RuntimeMember}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.permissions.PermissionDefinition
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import org.junit.Ignore
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentCourseDetails

class SecurityServiceTest extends TestBase with Mockito {
	
	val user = new User("cusebr")
	user.setIsLoggedIn(true)
	user.setFoundUser(true)
	
	@Test def godMode {
		val securityService = new SecurityService

		val currentUser = new CurrentUser(user, user, god=true)
		
		securityService.can(currentUser, Permissions.GodMode) should be (true)
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, new Department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(new StudentRelationshipType), null) should be (true)
		securityService.can(currentUser, null) should be (true)
		securityService.can(currentUser, null, null) should be (true)
	}
	
	@Test def explicitPermission {
		val securityService = new SecurityService
		
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val roleService = mock[RoleService]
		roleService.getExplicitPermissionsFor(currentUser, null) returns (Stream(
				PermissionDefinition(Permissions.UserPicker, None, true),
				PermissionDefinition(Permissions.ImportSystemData, None, true)
		))
		roleService.getExplicitPermissionsFor(currentUser, department) returns (Stream(
				PermissionDefinition(Permissions.Department.ManageDisplaySettings, Some(department), true)
		))
				
		securityService.roleService = roleService
		
		securityService.can(currentUser, Permissions.GodMode) should be (false)
		securityService.can(currentUser, Permissions.Module.Create, department) should be (false)
		securityService.can(currentUser, Permissions.UserPicker) should be (true)
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, department) should be (true)
	}
	
	@Test def role {
		val securityService = new SecurityService
		
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val roleService = mock[RoleService]
		roleService.getRolesFor(currentUser, null) returns (Stream(
			Sysadmin()
		))
		roleService.getRolesFor(currentUser, department) returns (Stream(
			Sysadmin(),
			DepartmentalAdministrator(department)
		))
				
		securityService.roleService = roleService
		
		securityService.can(currentUser, Permissions.UserPicker) should be (false)
		securityService.can(currentUser, Permissions.GodMode) should be (true)
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, department) should be (true)
		// Global perm
		securityService.can(currentUser, Permissions.Module.Create, department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(new StudentRelationshipType), null) should be (false)
	}
	
	@Test def globalPermission {
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> None
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, null
		) should be (securityService.Continue)
	}
	
	@Test def exactScopeMatch {
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> Some(department)
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Continue)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
	}
	
	@Test def scopelessMatch {
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> None
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
	}
	
	@Test def higherScopeMatch {
		val module = new Module
		val department = new Department
		module.department = department
		
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> Some(department)
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, module
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Assignment
		) should be (securityService.Continue)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, module
		) should be (securityService.Continue)
	}
	
	@Test def scopelessPermission {
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.GodMode -> None
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.GodMode, null
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.ImportSystemData, null
		) should be (securityService.Continue)
	}
	
	@Test def runtimeMemberDenied {
		val currentUser = new CurrentUser(user, user)
		val securityService = new SecurityService
		val runtimeMember = new RuntimeMember(currentUser)
		val realMember = new StaffMember
		
		securityService.checkRuntimeMember(
				currentUser, Permissions.Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]), runtimeMember
		) should be (securityService.Deny)
		
		securityService.checkRuntimeMember(
				currentUser, Permissions.Department.DownloadFeedbackReport, realMember
		) should be (securityService.Continue)
		
		securityService.checkRuntimeMember(
				currentUser, Permissions.Department.ManageDisplaySettings, runtimeMember
		) should be (securityService.Deny)
	}
	
	@Test def selectors {
		val type1 = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
		val type2 = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")
		
		val securityService = new SecurityService
		val currentUser = new CurrentUser(user, user)
		
		val department = new Department
		
		val student1 = new StudentMember
		student1.universityId = "1111111"
		student1.homeDepartment = department
		
		val student2 = new StudentMember
		student2.universityId = "2222222"
		student2.homeDepartment = department
		
		val studentCourseDetails = new StudentCourseDetails(student1, "1111111/1")
		
		/*
		 * I can:
		 * 
		 * - read any relationship type in the department
		 * - create type1, but only against that student
		 * - delete type2 over the whole department
		 */ 
		
		val deptPerms = Stream(
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), Some(department), true),
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Delete(type2), Some(department), true),
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Update(type1), Some(department), true),
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Update(type2), Some(department), true)
		)
		
		val student1Perms = Stream(
				PermissionDefinition(Permissions.Profiles.StudentRelationship.Create(type1), Some(student1), true)
		) #::: deptPerms
		
		val student2Perms = deptPerms
		
		val scdPerms = student1Perms
		
		val roleService = mock[RoleService]
		roleService.getExplicitPermissionsFor(currentUser, null) returns (Stream.empty)
		roleService.getExplicitPermissionsFor(currentUser, studentCourseDetails) returns (scdPerms)
		roleService.getExplicitPermissionsFor(currentUser, student1) returns (student1Perms)
		roleService.getExplicitPermissionsFor(currentUser, student2) returns (student2Perms)
		roleService.getExplicitPermissionsFor(currentUser, department) returns (deptPerms)
				
		securityService.roleService = roleService
		
		// Should be able to create type 1, but not type 2 or any, and only over student1
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type1), department) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type2), department) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(PermissionsSelector.Any[StudentRelationshipType]), department) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type1), student1) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type2), student1) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(PermissionsSelector.Any[StudentRelationshipType]), student1) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type1), studentCourseDetails) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type2), studentCourseDetails) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(PermissionsSelector.Any[StudentRelationshipType]), studentCourseDetails) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type1), student2) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(type2), student2) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Create(PermissionsSelector.Any[StudentRelationshipType]), student2) should be (false)
		
		// Can read any type over the dept
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), department) should be (true)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), student1) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), student1) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), student1) should be (true)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), studentCourseDetails) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), studentCourseDetails) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), studentCourseDetails) should be (true)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type1), student2) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(type2), student2) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), student2) should be (true)
		
		// Can delete type 2 only for whole dept
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type1), department) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type2), department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(PermissionsSelector.Any[StudentRelationshipType]), department) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type1), student1) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type2), student1) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(PermissionsSelector.Any[StudentRelationshipType]), student1) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type1), studentCourseDetails) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type2), studentCourseDetails) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(PermissionsSelector.Any[StudentRelationshipType]), studentCourseDetails) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type1), student2) should be (false)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(type2), student2) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Delete(PermissionsSelector.Any[StudentRelationshipType]), student2) should be (false)
		
		// Can edit type 1 and type 2, but not any
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type1), department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type2), department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(PermissionsSelector.Any[StudentRelationshipType]), department) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type1), student1) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type2), student1) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(PermissionsSelector.Any[StudentRelationshipType]), student1) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type1), studentCourseDetails) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type2), studentCourseDetails) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(PermissionsSelector.Any[StudentRelationshipType]), studentCourseDetails) should be (false)
		
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type1), student2) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(type2), student2) should be (true)
		securityService.can(currentUser, Permissions.Profiles.StudentRelationship.Update(PermissionsSelector.Any[StudentRelationshipType]), student2) should be (false)
	}
	
	/*
	 * Just testing a compiler warning that scared me, about it
	 * mismatching case classes that use inheritence. But it's
	 * only when the superclass is a case class, which we don't do. 
	 * I'll leave this in though!
	 */
	@Test def caseMatching {
	  val action = Permissions.GodMode
	  
	  (action:Any) match {
	    case Permissions.GodMode => {}
	    case _ => fail("Should have matched god mode")
	  }
	}
	
	/*
	 * Test that we don't accidentally make permissions of the same
	 * name equal to each other in future. Sigh 
	 */
	@Test def equality {
		(Permissions.Module.Create == Permissions.Feedback.Create) should be (false)
		(Permissions.Module.Create == Permissions.Module.Create) should be (true)
	}
			
}