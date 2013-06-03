package uk.ac.warwick.tabula.services

import scala.reflect._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.userlookup.{User, GroupService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.cache.{SingularCacheEntryFactory, Caches, CacheEntryFactory}
import java.util
import org.joda.time.Days

/**
 * Experimental. Class for getting a collection of entities that contain
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
 * FIXME needs cache dirtying (like PermissionsService)
 * TODO PermissionsService ought to use this when it is fully featured
 */
private[services] class UserGroupMembershipHelper[A <: Serializable : ClassTag](
		path: String,
		checkUniversityIds:Boolean = true) extends Daoisms {

	val groupService = Wire[GroupService]
	val userlookup = Wire[UserLookupService]

	val simpleEntityName = classTag[A].runtimeClass.getSimpleName

	val cacheName = simpleEntityName + "-" + path.replace(".","-")
	val cache = Caches.newCache(cacheName, new UserGroupMembershipCacheFactory(), Days.ONE.toStandardSeconds.getSeconds)

	private val pathParts = path.split("\\.").toList.reverse
	if (pathParts.size > 2) throw new IllegalArgumentException("Only allowed one or two parts to the path")
	val userProp = pathParts.head
	
	val groupsByUserSql = {
		val joinTable = pathParts.tail.headOption
		val leftJoin = joinTable.fold("")( table => s"left join r.$table as $table" )
		val prop = joinTable.fold("")(_ + ".") + userProp

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
			select distinct r
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

	protected def getUser(usercode: String) = userlookup.getUserByUserId(usercode)

	def findBy(user: User): Seq[A] = {
		// Pass just the user ID in even though it instantly gets resolved back
		// to a User object - we don't want to cache a whole User as a key.
		// Userlookup has its own cache so this shouldn't be expensive.
		cache.get(user.getUserId)
		findByInternal(user)
	}

	protected def findByInternal(user: User): Seq[A] = {
		val groupsByUser = session.newQuery[A](groupsByUserSql)
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.seq

		val webgroupNames: Seq[String] = groupService.getGroupsNamesForUser(user.getUserId).asScala

		val groupsByWebgroup = webgroupNames.flatMap { webgroupName =>
			session.newCriteria[A]
				.createAlias(path, path)
				.add(is(s"$path.baseWebgroup", webgroupName))
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


