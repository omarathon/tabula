package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.GroupService
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.courses.data.model.Module
import org.joda.time.DateTime
import org.joda.time.Duration
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.Features

@Controller class HomeController extends BaseController {
	@Autowired var moduleService: ModuleAndDepartmentService =_
	@Autowired var assignmentService: AssignmentService =_
	@Autowired var userLookup:UserLookupService =_
	@Autowired var features:Features =_
	def groupService = userLookup.getGroupService
	
	hideDeletedItems
  
	@RequestMapping(Array("/"))	def home(user:CurrentUser) = {
	  if (user.loggedIn) {
		  val moduleWebgroups = moduleService.modulesAttendedBy(user.idForPermissions)//groupsFor(user),
		  val ownedDepartments = moduleService.departmentsOwnedBy(user.idForPermissions)
		  val ownedModules = moduleService.modulesManagedBy(user.idForPermissions)
		  
		  val filter = session.getEnabledFilter("notDeleted")
		  
		  val assignmentsWithFeedback = assignmentService.getAssignmentsWithFeedback(user.universityId)
		  val assignmentsWithSubmission = 
		 	  if (features.submissions) assignmentService.getAssignmentsWithSubmission(user.universityId)
		 	  else Seq.empty
		  
		  if (false/*disabled*/ && moduleWebgroups.isEmpty && ownedModules.isEmpty && ownedDepartments.size == 1) {
		 	  debug("%s is just admin of %s, so redirecting straight there.", user, ownedDepartments.head)
		 	  Mav("redirect:/admin/department/%s/".format(ownedDepartments.head.code))
		  } else {
			  Mav("home/view",
			 	  "assignmentsWithFeedback" -> assignmentsWithFeedback,
			 	  "assignmentsWithSubmission" -> (assignmentsWithSubmission filterNot (assignmentsWithFeedback contains)),
			      "moduleWebgroups" -> webgroupsToMap(moduleWebgroups),
			      "ownedDepartments" -> ownedDepartments,
			      "ownedModule" -> ownedModules,
			      "ownedModuleDepartments" -> ownedModules.map{_.department}.distinct
			      )
		  }
	  } else {
	 	  Mav("home/view")
	  }
	}
	 
	def webgroupsToMap(groups:Seq[Group]) = groups
			.map {(g:Group)=> (Module.nameFromWebgroupName(g.getName), g) }
			.sortBy { _._1 }
	
		
}