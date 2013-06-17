package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition
import collection.JavaConverters._

class DepartmentTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]
	
	@Test def settings {
		val department = new Department
		department.collectFeedbackRatings.value should be (false)
		department.allowExtensionRequests.value should be (false)
		department.canRequestExtension.value should be (false)
		department.extensionGuidelineSummary.value should be (None)
		department.formattedGuidelineSummary should be ("")
		department.extensionGuidelineLink.value should be (None)
		department.showStudentName.value should be (false)
		department.plagiarismDetectionEnabled.value should be (true)
		
		department.collectFeedbackRatings.value = true
		department.allowExtensionRequests.value = true
		department.extensionGuidelineSummary.value = "Here is my magic summary.\n\n    Do everything good!"
		department.extensionGuidelineLink.value = "http://warwick.ac.uk"
		department.showStudentName.value = true
		department.plagiarismDetectionEnabled.value = false
		
		department.collectFeedbackRatings.value should be (true)
		department.allowExtensionRequests.value should be (true)
		department.canRequestExtension.value should be (true)
		department.extensionGuidelineSummary.value should be ("Here is my magic summary.\n\n    Do everything good!")
		department.formattedGuidelineSummary should be ("<p>Here is my magic summary.</p><p>Do everything good!</p>")
		department.extensionGuidelineLink.value should be ("http://warwick.ac.uk")
		department.showStudentName.value should be (true)
		department.plagiarismDetectionEnabled.value should be (false)
	}
	
	@Test def groups {
		val department = new Department
		department.permissionsService = permissionsService
		
		val ownersGroup = UserGroup.emptyUsercodes
		val extmanGroup = UserGroup.emptyUsercodes
		
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