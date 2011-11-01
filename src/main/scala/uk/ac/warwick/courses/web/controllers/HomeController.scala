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

@Controller class HomeController {
	@Autowired var moduleService: ModuleAndDepartmentService =_
	@Autowired var groupService: GroupService =_
  
	@RequestMapping(Array("/"))	def home(user:CurrentUser) =
	  Mav("home/view",
	      "moduleWebgroups" -> groupsForUser(user)
	      )
	      
	def groupsForUser(user:CurrentUser) = 
		if (user.loggedIn) filterGroups(groupService.getGroupsForUser(user.idForPermissions))
		else Nil
	      
	/**
	 * Filter groups down to module types,
	 * sort by name
	 */
	def filterGroups(groups:Seq[Group]) = groups
		.filter { "Module" equals _.getType }
		.map {(g:Group)=> (Module.nameFromWebgroupName(g.getName), g) }
		.sortBy { _._1 }
		
}