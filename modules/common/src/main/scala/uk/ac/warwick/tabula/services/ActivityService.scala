package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.Activity
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Service

@Service
class ActivityService {
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var assignmentService = Wire.auto[AssignmentService]
	var auditIndexService = Wire.auto[AuditEventIndexService]

	var userLookup = Wire.auto[UserLookupService]

	/** At the moment, this is only going to gather new submission events.
	 *  In the future it'll likely make sense service events of interest at whichever
	 *  depth of Tabula we're looking from.
	 * */
	def getActivities(user: CurrentUser): Seq[Activity[Any]] = {
		val ownedModules = moduleService.modulesManagedBy(user.idForPermissions).toSet
		val adminModules = moduleService.modulesAdministratedBy(user.idForPermissions).toSet
		val collatedModules = (ownedModules ++ adminModules).toSeq
		
		auditIndexService.recentSubmissionsForModules(collatedModules) flatMap (event => Activity(event))
	}

}