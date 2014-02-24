package uk.ac.warwick.tabula.services

import scala.reflect._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.userlookup.{User, GroupService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.cache.{Caches, SingularCacheEntryFactory}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.{KnownTypeUserGroup, UnspecifiedTypeUserGroup}
import org.joda.time.Days
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import org.springframework.stereotype.Service
import uk.ac.warwick.util.queue.{Queue, QueueListener}
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.util.queue.conversion.ItemType
import org.codehaus.jackson.annotate.JsonAutoDetect

trait UserGroupMembershipHelperMethods[A <: Serializable] {
	def findBy(user: User): Seq[A]
}

/**
 * Class for getting a collection of entities that contain
 * a UserGroup that holds a given user. For example, you could use
 * UserGroupMembershipHelper[SmallGroupEvent]("tutors") to access small group
 * events for which user X is a tutor.
 *
 * The path can have a second part if the object goes through yet another object,
 * like [SmallGroup]("events.tutors"). We could technically allow longer paths
 * but it doesn't support that currently and if you want to do this you should
 * maybe think about whether this would be a bit much to ask of the database.
 *
 * This will cache under the default EHcache config unless you define a cache
 * called ClassName-dashed-path,
 * e.g. SmallGroup-events-tutors or SmallGroupEvent-tutors.
 *
 * TODO PermissionsService ought to use this when it is fully featured
 */
private[services] class UserGroupMembershipHelper[A <: Serializable : ClassTag] (
		path: String,
		checkUniversityIds:Boolean = true)
	extends UserGroupMembershipHelperMethods[A] with Daoisms {

	var groupService = Wire[GroupService]
	var userlookup = Wire[UserLookupService]

	val simpleEntityName = classTag[A].runtimeClass.getSimpleName

	val cacheName = simpleEntityName + "-" + path.replace(".","-")
	val cache = Caches.newCache(cacheName, new UserGroupMembershipCacheFactory(), Days.ONE.toStandardSeconds.getSeconds, CacheStrategy.EhCacheIfAvailable)

  // A series of confusing variables for building joined queries across paths split by.dots
	private val pathParts = path.split("\\.").toList.reverse
	if (pathParts.size > 2) throw new IllegalArgumentException("Only allowed one or two parts to the path")
  // The actual name of the UserGroup
	val usergroupName = pathParts.head
  // A possible table to join through to get to userProp
  val joinTable: Option[String] = pathParts.tail.headOption
  // The overall property name, possibly including the joinTable
  val prop: String = joinTable.fold("")(_ + ".") + usergroupName
	
	val groupsByUserSql = {
    val leftJoin = joinTable.fold("")( table => s"left join r.$table as $table" )

		// skip the university IDs check if we know we only ever use usercodes
		val universityIdsClause =
			if (checkUniversityIds) s""" or (
					$prop.universityIds = true and
					((:universityId in elements($prop.staticIncludeUsers)
					or :universityId in elements($prop.includeUsers))
					and :universityId not in elements($prop.excludeUsers))
				)"""
			else ""

		s"""
			select r
			from $simpleEntityName r
			$leftJoin
			where
				(
					$prop.universityIds = false and
					((:userId in elements($prop.staticIncludeUsers)
					or :userId in elements($prop.includeUsers))
					and :userId not in elements($prop.excludeUsers))
				) $universityIdsClause
		"""
	}

  // To override in tests
	protected def getUser(usercode: String) = userlookup.getUserByUserId(usercode)
	protected def getWebgroups(usercode: String): Seq[String] = usercode.maybeText.map { usercode => groupService.getGroupsNamesForUser(usercode).asScala }.getOrElse(Nil)

	def findBy(user: User): Seq[A] = {
		cache.get(user.getUserId)
	}

	protected def findByInternal(user: User): Seq[A] = {
		val groupsByUser = session.newQuery[A](groupsByUserSql)
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.distinct
			.seq

		val webgroupNames: Seq[String] = getWebgroups(user.getUserId)
		val groupsByWebgroup =
			if (webgroupNames.isEmpty) Nil
			else {
				val criteria = session.newCriteria[A]

				joinTable.foreach { table =>
					criteria.createAlias(table, table)
				}

				criteria
					.createAlias(prop, "usergroupAlias")
					.add(safeIn("usergroupAlias.baseWebgroup", webgroupNames))
					.seq
			}

		(groupsByUser ++ groupsByWebgroup).distinct
	}

	private class UserGroupMembershipCacheFactory extends SingularCacheEntryFactory[String, Array[A]] {
		def create(usercode: String) = {
			val user = getUser(usercode)
			findByInternal(user).toArray[A]
		}

		def shouldBeCached(value: Array[A]) = true
	}
}

@Service(value = "userGroupMembershipHelperCacheService")
class UserGroupMembershipHelperCacheService extends QueueListener with InitializingBean {

	var queue = Wire.named[Queue]("settingsSyncTopic")

	def invalidate(helper: UserGroupMembershipHelper[_], user: User) {
		helper.cache.remove(user.getUserId)

		// Must also inform other Jbosses
		val msg = new UserGroupMembershipHelperCacheBusterMessage
		msg.cacheName = helper.cache.getName
		msg.usercode = user.getUserId
		queue.send(msg)
	}

	override def isListeningToQueue = true
	override def onReceive(item: Any) {
		item match {
			case msg: UserGroupMembershipHelperCacheBusterMessage => {
				Caches.newCache(msg.cacheName, null, Int.MaxValue).remove(msg.usercode)
			}
			case _ =>
		}
	}

	override def afterPropertiesSet() {
		queue.addListener(classOf[UserGroupMembershipHelperCacheBusterMessage].getAnnotation(classOf[ItemType]).value, this)
	}
}

@ItemType("UserGroupMembershipHelperCacheBuster")
@JsonAutoDetect
class UserGroupMembershipHelperCacheBusterMessage {
	@BeanProperty var cacheName: String = _
	@BeanProperty var usercode: String = _
}

class UserGroupCacheManager(val underlying: UnspecifiedTypeUserGroup, helper: UserGroupMembershipHelper[_]) extends UnspecifiedTypeUserGroup with KnownTypeUserGroup {

	// FIXME this isn't really an optional wire, it's just represented as such to make testing easier
	var cacheService = Wire.option[UserGroupMembershipHelperCacheService]
	var userLookup = Wire[UserLookupService]

	def add(user: User) {
		underlying.add(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}
	def remove(user: User) {
		underlying.remove(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}
	def exclude(user: User) {
		underlying.exclude(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}
	def unexclude(user: User) {
		underlying.unexclude(user)
		cacheService.foreach { _.invalidate(helper, user) }
	}

	private def getUserFromUserId(userId: String): User =
		if (universityIds) userLookup.getUserByWarwickUniIdUncached(userId)
		else userLookup.getUserByUserId(userId)

	def addUserId(userId: String) = {
		underlying.knownType.addUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def removeUserId(userId: String) = {
		underlying.knownType.removeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def excludeUserId(userId: String) = {
		underlying.knownType.excludeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def unexcludeUserId(userId: String) = {
		underlying.knownType.unexcludeUserId(userId)
		cacheService.foreach { _.invalidate(helper, getUserFromUserId(userId)) }
	}
	def staticUserIds_=(userIds: Seq[String]) = {
		underlying.knownType.staticUserIds = userIds

		for (cacheService <- cacheService; user <- userIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def includedUserIds_=(userIds: Seq[String]) = {
		underlying.knownType.includedUserIds = userIds

		for (cacheService <- cacheService; user <- userIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}
	def excludedUserIds_=(userIds: Seq[String]) = {
		underlying.knownType.excludedUserIds = userIds

		for (cacheService <- cacheService; user <- userIds.map(getUserFromUserId))
			cacheService.invalidate(helper, user)
	}

	def users = underlying.users
	def excludes = underlying.excludes
	def size = underlying.size
	def isEmpty = underlying.isEmpty
	def includesUser(user: User) = underlying.includesUser(user)
	def excludesUser(user: User) = underlying.excludesUser(user)
	def hasSameMembersAs(other: UnspecifiedTypeUserGroup) = underlying.hasSameMembersAs(other)
	def copyFrom(otherGroup: UnspecifiedTypeUserGroup) = underlying.copyFrom(otherGroup)
	def duplicate() = new UserGroupCacheManager(underlying.duplicate(), helper) // Should this be wrapped or not?
	val universityIds = underlying.universityIds
	def knownType = underlying.knownType
	def allIncludedIds = underlying.knownType.allIncludedIds
	def allExcludedIds = underlying.knownType.allExcludedIds
	def members = underlying.knownType.members
	def includedUserIds = underlying.knownType.includedUserIds
	def excludedUserIds = underlying.knownType.excludedUserIds
	def staticUserIds = underlying.knownType.staticUserIds
	def includesUserId(userId: String) = underlying.knownType.includesUserId(userId)
	def excludesUserId(userId: String) = underlying.knownType.excludesUserId(userId)

}