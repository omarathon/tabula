package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{StudentSignUp, Manual}

class DepartmentTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]
	
	@Test def settings {
		val department = new Department
		department.collectFeedbackRatings should be (false)
		department.allowExtensionRequests should be (false)
		department.canRequestExtension should be (false)
		department.extensionGuidelineSummary should be (null)
		department.formattedGuidelineSummary should be ("")
		department.extensionGuidelineLink should be (null)
		department.showStudentName should be (false)
		department.plagiarismDetectionEnabled should be (true)
    department.defaultGroupAllocationMethod should be (Manual)

		department.collectFeedbackRatings = true
		department.allowExtensionRequests = true
		department.extensionGuidelineSummary = "Here is my magic summary.\n\n    Do everything good!"
		department.extensionGuidelineLink = "http://warwick.ac.uk"
		department.showStudentName = true
		department.plagiarismDetectionEnabled = false
    department.defaultGroupAllocationMethod = StudentSignUp
		
		department.collectFeedbackRatings should be (true)
		department.allowExtensionRequests should be (true)
		department.canRequestExtension should be (true)
		department.extensionGuidelineSummary should be ("Here is my magic summary.\n\n    Do everything good!")
		department.formattedGuidelineSummary should be ("<p>Here is my magic summary.</p><p>Do everything good!</p>")
		department.extensionGuidelineLink should be ("http://warwick.ac.uk")
		department.showStudentName should be (true)
		department.plagiarismDetectionEnabled should be (false)
    department.defaultGroupAllocationMethod should be (StudentSignUp)
	}
	
	@Test def groups {
		val department = new Department
		department.permissionsService = permissionsService
		
		val ownersGroup = UserGroup.ofUsercodes
		val extmanGroup = UserGroup.ofUsercodes
		
		permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns (ownersGroup)
		permissionsService.ensureUserGroupFor(department, ExtensionManagerRoleDefinition) returns (extmanGroup)
		
		department.isOwnedBy("cuscav") should be (false)
		
		department.owners.addUser("cuscav")
		department.owners.addUser("cusebr")
		department.owners.addUser("curef")
		
		department.owners.removeUser("cusebr")
		
		department.isOwnedBy("cuscav") should be (true)
		department.isOwnedBy("curef") should be (true)
		department.isOwnedBy("cusebr") should be (false)
		
		ownersGroup.includeUsers.asScala.toSeq should be (Seq("cuscav", "curef"))
		
		department.isExtensionManager("cuscav") should be (false)
		extmanGroup.addUser("cuscav")
		department.isExtensionManager("cuscav") should be (true)
	}

}