package uk.ac.warwick.tabula.data.model;

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import org.hibernate.annotations.AccessType

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
@AccessType("field")
class UserGroup private(val universityIds: Boolean) extends GeneratedId with UnspecifiedTypeUserGroup{

	/* For Hibernate xx */
	private def this() { this(false) }

	@transient var userLookup = Wire.auto[UserLookupService]
	def groupService = userLookup.getGroupService

	var baseWebgroup: String = _

	def baseWebgroupSize = groupService.getGroupInfo(baseWebgroup).getSize()

	@ElementCollection @Column(name = "usercode")
	@JoinTable(name = "UserGroupInclude", joinColumns = Array(
		new JoinColumn(name = "group_id", referencedColumnName = "id")))
	var includeUsers: JList[String] = JArrayList()

	@ElementCollection @Column(name = "usercode")
	@JoinTable(name = "UserGroupStatic", joinColumns = Array(
		new JoinColumn(name = "group_id", referencedColumnName = "id")))
	var staticIncludeUsers: JList[String] = JArrayList()

	@ElementCollection @Column(name = "usercode")
	@JoinTable(name = "UserGroupExclude", joinColumns = Array(
		new JoinColumn(name = "group_id", referencedColumnName = "id")))
	var excludeUsers: JList[String] = JArrayList()

	def add(user:User) = {
		if (universityIds) addUser(user.getWarwickId)
		else addUser(user.getUserId)
	}
	def addUser(user: String) = {
		if (!includeUsers.contains(user)) {
			includeUsers.add(user)
		} else false
	}
	def removeUser(user: String) = includeUsers.remove(user)

	def remove(user:User) = {
		if (universityIds) removeUser(user.getWarwickId)
		else removeUser(user.getUserId)
	}

	def excludeUser(user: String) = {
		if (!excludeUsers.contains(user)) {
			excludeUsers.add(user)
		} else false
	}
	def unexcludeUser(user: String) = excludeUsers.remove(user)


	/*
	 * Could implement as `members.contains(user)`
	 * but this is more efficient
	 */
	def includes(user: String) =
		!(excludeUsers contains user) &&
			(
				(includeUsers contains user) ||
				(staticIncludeUsers contains user) ||
				(baseWebgroup != null && groupService.isUserInGroup(user, baseWebgroup)))

	def includesUser(user:User) = {
		if (universityIds) includes(user.getWarwickId)
		else includes(user.getUserId)
	}
	def isEmpty = members.isEmpty
	def size = members.size

	def members: Seq[String] =
		(includeUsers.toList ++ staticIncludeUsers ++ webgroupMembers) filterNot excludeUsers.contains

	def users: Seq[User] =
		if (universityIds) members map {
			userLookup.getUserByWarwickUniId(_)
		}
		else {
			if (members.isEmpty) {
				Nil
			} else {
				userLookup.getUsersByUserIds(members.asJava).values.asScala.toSeq
			}
		}

	def webgroupMembers: List[String] = baseWebgroup match {
		case webgroup: String => groupService.getUserCodesInGroup(webgroup).asScala.toList
		case _ => Nil
	}


	def copyFrom(otherGroup: UnspecifiedTypeUserGroup) {
		otherGroup match {
			case other:UserGroup=>{
				assert(this.universityIds == other.universityIds, "Can only copy from a group with same type of users")
				baseWebgroup = other.baseWebgroup
				includeUsers.clear()
				excludeUsers.clear()
				staticIncludeUsers.clear()
				includeUsers.addAll(other.includeUsers)
				excludeUsers.addAll(other.excludeUsers)
				staticIncludeUsers.addAll(other.staticIncludeUsers)
			}
			case _ => {
				assert(false, "Can only copy from one UserGroup to another")
			}
		}

	}

	def duplicate(): UserGroup = {
		val newGroup = new UserGroup(this.universityIds)
		newGroup.copyFrom(this)
		newGroup.userLookup = this.userLookup
		newGroup
	}

	def hasSameMembersAs(other:UnspecifiedTypeUserGroup):Boolean ={
		other match {
			case otherUg:UserGroup if otherUg.universityIds == this.universityIds=> (this.members == otherUg.members)
			case _ => this.users == other.users
		}
	}


}

object UserGroup {
	def ofUsercodes = new UserGroup(false)
	def ofUniversityIds = new UserGroup(true)
}

/**
 * A usergroup where the value of universityId is hidden from the caller.
 *
 * This means that callers can only add/remove Users, not UserIds.
 */

trait UnspecifiedTypeUserGroup{
	def users: Seq[User]
	def add(User:User)
	def remove(User:User)
	def size:Int
	def isEmpty:Boolean
	def hasSameMembersAs(other:UnspecifiedTypeUserGroup):Boolean
}
