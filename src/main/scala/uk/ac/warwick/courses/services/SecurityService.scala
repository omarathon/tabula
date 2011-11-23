package uk.ac.warwick.courses.services
import uk.ac.warwick.userlookup.GroupService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.util.core.StringUtils._
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.CurrentUser
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.PermissionDeniedException
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.helpers.Logging

/**
 * Checks permissions.
 */
@Service
class SecurityService extends Logging {
	@Autowired var groupService:GroupService =_
  
	def isSysadmin(usercode:String) = hasText(usercode) && groupService.isUserInGroup(usercode, "in-courses-sysadmins") 
	
	/*
	 * In Java we'd define an interface for a PermissionChecker with one method,
	 * but we'll just define a type alias so we can implement each check as a single method.
	 */
	type PermissionChecker = (CurrentUser, Action) => Boolean
	val checks = List[PermissionChecker](checkSysadmin _, checkGroup _)
	
	def checkSysadmin(user:CurrentUser, action:Action):Boolean = user.sysadminEnabled
	
	def checkGroup(user:CurrentUser, action:Action):Boolean = action match {
		
	  case Manage(department:Department) => department isOwnedBy user.idForPermissions
	  case View(department:Department) => checkGroup(user, Manage(department))
	  
	  case View(module:Module) => module.getMembers().includes(user.apparentId) || 
	  							  checkGroup(user, View(module.department))
	  							  
	  case View(assignment:Assignment) => checkGroup(user, View(assignment.module))
	  case Submit(assignment:Assignment) => checkGroup(user, View(assignment.module))
	  
	  case _ => throw new IllegalArgumentException()
	   
	}
	
	/**
	 * Returns whether the given user can do the given Action on the object
	 * specified by the Action.
	 */
	@Transactional(readOnly=true)
	def can(user:CurrentUser, action:Action):Boolean = {
		// loop through checks, seeing if any of them return true.
	    val canDo:Boolean = checks.find{ _(user,action) }.isDefined
	    if (debugEnabled) logger.debug("can "+user+" do "+action+"? " + (if(canDo) "Yes" else "NO"))
	    canDo
	}
	
	
	
	def check(user:CurrentUser, action:Action) = can(user,action) match {
	  case true => {} //continue
	  case false => throw new PermissionDeniedException(user, action)
	}
}
