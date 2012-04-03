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
	@Autowired var userLookup:UserLookupService =_
	
	def groupService = userLookup.getGroupService
  
	def isSysadmin(usercode:String) = hasText(usercode) && groupService.isUserInGroup(usercode, "in-courses-sysadmins")
	// excludes sysadmins, though they can also masquerade
	def isMasquerader(usercode:String) = hasText(usercode) && groupService.isUserInGroup(usercode, "in-courses-hasmasque")
	
	/*
	 * In Java we'd define an interface for a PermissionChecker with one method,
	 * but we'll just define a type alias so we can implement each check as a single method.
	 */
	type PermissionChecker = (CurrentUser, Action[_]) => Boolean
	val checks = List[PermissionChecker](checkSysadmin _, checkGroup _)
	
	def checkSysadmin(user:CurrentUser, action:Action[_]):Boolean = user.god
	
	def checkGroup(user:CurrentUser, action:Action[_]):Boolean = action match {
		
	  case Manage(department:Department) => department isOwnedBy user.idForPermissions
	  case View(department:Department) => can(user, Manage(department))
	  
	  // Participate module = can submit feedback, publish feedback, add/create/delete assignments
	  case Participate(module:Module) => module.ensuredParticipants.includes(user.apparentId) || 
	 	  						  	can(user, Manage(module.department))
	  // Manage module = can modify its permissions.
	  case Manage(module:Module) => can(user, Manage(module.department))
	  // View module = see what assignments are in a module
	  case View(module:Module) => module.ensuredMembers.includes(user.apparentId) || 
	  							  can(user, View(module.department))
	  
	  case View(assignment:Assignment) => can(user, View(assignment.module))
	  case Submit(assignment:Assignment) => can(user, View(assignment.module))
	  
	  case View(feedback:Feedback) => feedback.universityId == user.universityId ||
	  								can(user, View(feedback.assignment))
	  case Delete(feedback:Feedback) => can(user, Participate(feedback.assignment.module))
	  
	  case Delete(submission:Submission) => can(user, Participate(submission.assignment.module))
	  
	  case Masquerade() => user.sysadmin || user.masquerader
	  
	  case action:Action[_] => throw new IllegalArgumentException(action.toString)
	  case _ => throw new IllegalArgumentException()
	   
	}
	
	/**
	 * Returns whether the given user can do the given Action on the object
	 * specified by the Action.
	 */
	@Transactional(readOnly=true)
	def can(user:CurrentUser, action:Action[_]):Boolean = {
		// loop through checks, seeing if any of them return true.
	    val canDo:Boolean = checks.find{ _(user,action) }.isDefined
	    if (debugEnabled) logger.debug("can "+user+" do "+action+"? " + (if(canDo) "Yes" else "NO"))
	    canDo
	}
	
	
	
	def check(user:CurrentUser, action:Action[_]) = can(user,action) match {
	  case true => {} //continue
	  case false => throw new PermissionDeniedException(user, action)
	}
}
