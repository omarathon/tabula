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
import uk.ac.warwick.tabula.helpers.StringUtils._

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
class UserGroup private(val universityIds: Boolean) extends GeneratedId with UnspecifiedTypeUserGroup with KnownTypeUserGroup {

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
		addUser(getIdFromUser(user))
	}
	def addUser(user: String) = {
		if (!includeUsers.contains(user) && user.hasText) {
			includeUsers.add(user)
		} else false
	}
	def removeUser(user: String) = includeUsers.remove(user)

	def remove(user:User) = {
		removeUser(getIdFromUser(user))
	}

	def excludeUser(user: String) = {
		if (!excludeUsers.contains(user) && user.hasText) {
			excludeUsers.add(user)
		} else false
	}
	def exclude(user:User)={
		excludeUser(getIdFromUser(user))
	}
	def unexcludeUser(user: String) = excludeUsers.remove(user)
  def unexclude(user:User)={
		unexcludeUser(getIdFromUser(user))
	}

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
		
	def allIncludedIds: Seq[String] = (includeUsers.asScala.toSeq ++ staticIncludeUsers.asScala ++ webgroupMembers)
	def allExcludedIds: Seq[String] = excludeUsers.asScala.toSeq

	private def getIdFromUser(user:User):String = {
		if (universityIds)
			user.getWarwickId
		else
			user.getUserId
	}
	private def getUsersFromIds(ids: Seq[String]): Seq[User] = ids match {
		case Nil => Nil
		case ids if universityIds => userLookup.getUsersByWarwickUniIds(ids).values.toSeq
		case ids => userLookup.getUsersByUserIds(ids.asJava).values.asScala.toSeq
	}

	def users: Seq[User] = getUsersFromIds(members)

	def excludes: Seq[User] = getUsersFromIds(excludeUsers)

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
	
	def knownType = this
}

object UserGroup {
	def ofUsercodes = new UserGroup(false)
	def ofUniversityIds = new UserGroup(true)
}

/**
 * A usergroup where the value of universityId is hidden from the caller.
 *
 * This means that callers can only add/remove Users, not UserIds/UniversityIds - and therefore they can't add the
 * wrong type of identifier.
 *
 */

trait UnspecifiedTypeUserGroup {
	/**
	 * @return All of the included users (includedUsers, staticUsers, and webgroup members), minus the excluded users
	 */
	def users: Seq[User]

	/**
	 * @return The explicitly excluded users
	 */
	def excludes: Seq[User]
	def add(User:User)
	def remove(user:User)
	def exclude(user:User)
	def unexclude(user:User)
	def size:Int
	def isEmpty:Boolean
  def includesUser(user:User):Boolean
	/**
	 * @return true if the other.users() would return the same values as this.users(), else false
	 */
	def hasSameMembersAs(other:UnspecifiedTypeUserGroup): Boolean
	
	val universityIds: Boolean
	def knownType: KnownTypeUserGroup
}

trait KnownTypeUserGroup {
	def allIncludedIds: Seq[String]
	def allExcludedIds: Seq[String]
}