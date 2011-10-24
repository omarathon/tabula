package uk.ac.warwick.courses.services
import uk.ac.warwick.userlookup.GroupService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.util.core.StringUtils._
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.CurrentUser
import org.springframework.transaction.annotation.Transactional

abstract class Action
case class View(obj:Any) extends Action
case class Submit(a:Assignment) extends Action
case class Manage(obj:Any) extends Action

/**
 * Checks permissions.
 */
@Service
class SecurityService {
	@Autowired var groupService:GroupService =_
  
	def isSysadmin(usercode:String) = hasText(usercode) && groupService.isUserInGroup(usercode, "in-courses-sysadmins")
	
	//def isSysadminActive = 
	
	/**
	 * Returns whether the given user can do the given Action on the object
	 * specified by the Action.
	 */
	@Transactional(readOnly=true)
	def can(user:CurrentUser, action:Action):Boolean = action match {
	  case View(department:Department) => false
	  case View(module:Module) => can(user, View(module.department))
	  case View(assignment:Assignment) => false
	  case _ => throw new IllegalArgumentException()
	}
}
