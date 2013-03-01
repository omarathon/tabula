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
		department.isCollectFeedbackRatings should be (false)
		department.isAllowExtensionRequests should be (false)
		department.canRequestExtension should be (false)
		department.getExtensionGuidelineSummary should be (null)
		department.formattedGuidelineSummary should be ("")
		department.getExtensionGuidelineLink should be (null)
		department.isShowStudentName should be (false)
		department.isPlagiarismDetectionEnabled should be (true)
		
		department.collectFeedbackRatings = true
		department.allowExtensionRequests = true
		department.extensionGuidelineSummary = "Here is my magic summary.\n\n    Do everything good!"
		department.extensionGuidelineLink = "http://warwick.ac.uk"
		department.showStudentName = true
		department.plagiarismDetectionEnabled = false
		
		department.isCollectFeedbackRatings should be (true)
		department.isAllowExtensionRequests should be (true)
		department.canRequestExtension should be (true)
		department.getExtensionGuidelineSummary should be ("Here is my magic summary.\n\n    Do everything good!")
		department.formattedGuidelineSummary should be ("<p>Here is my magic summary.</p><p>Do everything good!</p>")
		department.getExtensionGuidelineLink should be ("http://warwick.ac.uk")
		department.isShowStudentName should be (true)
		department.isPlagiarismDetectionEnabled should be (false)
	}
	
	@Test def groups {
		val department = new Department
		department.permissionsService = permissionsService
		
		val ownersGroup = UserGroup.emptyUsercodes
		val extmanGroup = UserGroup.emptyUsercodes
		
		permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns (ownersGroup)
		permissionsService.ensureUserGroupFor(department, ExtensionManagerRoleDefinition) returns (extmanGroup)
		
		department.isOwnedBy("cuscav") should be (false)
		
		department.addOwner("cuscav")
		department.addOwner("cusebr")
		department.addOwner("curef")
		
		department.removeOwner("cusebr")
		
		department.isOwnedBy("cuscav") should be (true)
		department.isOwnedBy("curef") should be (true)
		department.isOwnedBy("cusebr") should be (false)
		
		ownersGroup.includeUsers.asScala.toSeq should be (Seq("cuscav", "curef"))
		
		department.isExtensionManager("cuscav") should be (false)
		extmanGroup.addUser("cuscav")
		department.isExtensionManager("cuscav") should be (true)
	}

}