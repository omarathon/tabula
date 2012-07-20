package uk.ac.warwick.courses.data.model;

import javax.persistence.Entity
import scala.reflect.BeanProperty
import uk.ac.warwick.userlookup.GroupService
import org.springframework.beans.factory.annotation.Autowired
import org.hibernate.annotations.Filter
import javax.persistence._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.{util => jutil}
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.helpers.ArrayList
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.FilterDef

/**
 * Wherever a group of users is referenced in the app, it will be
 * stored as a UserGroup.
 * 
 * A UserGroup can either be a totally internal list of users, or it
 * can use a webgroup as a base and then specify users to add and
 * users to exclude from that base. 
 * 
 * When a webgroup is used, it is a live view on the webgroup (other
 * than the included and excluded users and caching), so it will
 * change when the webgroup does (caches permitting).
 * 
 * Depending on what the UserGroup is attached to, the UI might choose
 * not to allow a webgroup to be used, and only allow included users.
 * We might want to subclass UserGroup to make it a bit more explicit which
 * groups support Webgroups, and prevent invalid operations.
 * 
 * Depending on context, the usercodes may be university IDs.
 */
@Entity
@Configurable
class UserGroup extends GeneratedId {
  
	// Not created by Spring but @Autowiring works thanks to compile-time weaving.
	@Autowired @transient private var userLookup:UserLookupService =_
	def groupService = userLookup.getGroupService
  
	@BeanProperty var baseWebgroup:String =_
	
	def baseWebgroupSize = groupService.getGroupInfo(baseWebgroup).getSize()
	
	@ElementCollection @Column(name="usercode")
	@JoinTable(name="UserGroupInclude", joinColumns=Array(
	    new JoinColumn(name="group_id", referencedColumnName="id")
	))
	@BeanProperty var includeUsers:jutil.List[String] = ArrayList()
	
	@ElementCollection @Column(name="usercode")
	@JoinTable(name="UserGroupStatic", joinColumns=Array(
	    new JoinColumn(name="group_id", referencedColumnName="id")
	))
	@BeanProperty var staticIncludeUsers:jutil.List[String] = ArrayList()
	
	@ElementCollection @Column(name="usercode")
	@JoinTable(name="UserGroupExclude", joinColumns=Array(
	    new JoinColumn(name="group_id", referencedColumnName="id")
	))
	@BeanProperty var excludeUsers:jutil.List[String] = ArrayList()
	
	def addUser(user:String) = includeUsers.add(user)
	def removeUser(user:String) = includeUsers.remove(user)
	
	def excludeUser(user:String) = excludeUsers.add(user)
	def unexcludeUser(user:String) = excludeUsers.remove(user)

	var universityIds:Boolean = false
	
	/*
	 * Could implement as `members.contains(user)`
	 * but this is more efficient
	 */
	def includes (user:String) =
	  !(excludeUsers contains user) && 
	  (
	    (includeUsers contains user) ||
	    (staticIncludeUsers contains user) ||
	    (baseWebgroup != null && groupService.isUserInGroup(user, baseWebgroup))
	  )
	  
	def isEmpty = members.isEmpty
	  
	def members: Seq[String] =
	  (includeUsers.toList ++ staticIncludeUsers ++ webgroupMembers) filterNot excludeUsers.contains
	  
	def webgroupMembers:List[String] = baseWebgroup match {
	  case webgroup:String => groupService.getUserCodesInGroup(webgroup).asScala.toList
	  case _ => Nil
	}
	
	def copyFrom(other:UserGroup) {
		baseWebgroup = other.baseWebgroup
		includeUsers.clear
		excludeUsers.clear
		includeUsers.addAll(other.includeUsers)
		excludeUsers.addAll(other.excludeUsers)
	}
	
}

object UserGroup {
	def emptyUsercodes = new UserGroup
	def emptyUniversityIds = {
		val g = new UserGroup
		g.universityIds = true
		g
	}
}