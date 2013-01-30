package uk.ac.warwick.tabula.roles

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permission._

case class DepartmentalAdministrator(department: model.Department) extends BuiltInRole {
	
	// Implicitly grants module manager role for all modules in this department
	for (module <- department.modules)
		GrantsRole(ModuleManager(module))
		
	GrantsPermissionFor(department, 
		Department.ManageExtensionSettings(),
		Department.ManageDisplaySettings(),
		Department.DownloadFeedbackReport(),
		Department.ManagePermissions(),
		
		Module.ManagePermissions(),
		Assignment.ImportFromExternalSystem(),
		
		FeedbackTemplate.Create(),
		FeedbackTemplate.Read(),
		FeedbackTemplate.Update(),
		FeedbackTemplate.Delete(),
		
		MarkScheme.Create(),
		MarkScheme.Read(),
		MarkScheme.Update(),
		MarkScheme.Delete(),
		
		Profiles.PersonalTutor.Create(),
		Profiles.PersonalTutor.Read(),
		Profiles.PersonalTutor.Update(),
		Profiles.PersonalTutor.Delete()
	)

}