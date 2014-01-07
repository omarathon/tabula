package uk.ac.warwick.tabula.services

import scala.reflect._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.userlookup.{User, GroupService}
import uk.ac.warwick.spring.Wire
import org.hibernate.criterion.Restrictions._
import uk.ac.warwick.util.cache.SingularCacheEntryFactory

trait UserGroupMembershipHelperMethods[A <: Serializable] {
	def findBy(user: User): Seq[A]
}

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
private[services] class UserGroupMembershipHelper[A <: Serializable : ClassTag] (
		path: String,
		checkUniversityIds:Boolean = true)
	extends UserGroupMembershipHelperMethods[A] with Daoisms {

	val groupService = Wire[GroupService]
	val userlookup = Wire[UserLookupService]

	val simpleEntityName = classTag[A].runtimeClass.getSimpleName

	val cacheName = simpleEntityName + "-" + path.replace(".","-")
	//val cache = Caches.newCache(cacheName, new UserGroupMembershipCacheFactory(), Days.ONE.toStandardSeconds.getSeconds, CacheStrategy.EhCacheRequired)

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
	protected def getWebgroups(usercode: String): Seq[String] = groupService.getGroupsNamesForUser(usercode).asScala

	def findBy(user: User): Seq[A] = {		
		// Caching disabled for the moment until we have a proper cache dirtying strategy
		//cache.get(user.getUserId)
		findByInternal(user)
	}

	protected def findByInternal(user: User): Seq[A] = {
		val groupsByUser = session.newQuery[A](groupsByUserSql)
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.distinct
			.seq

		val webgroupNames: Seq[String] = getWebgroups(user.getUserId)

		val groupsByWebgroup = webgroupNames.grouped(Daoisms.MaxInClauseCount).flatMap { names =>
			val criteria = session.newCriteria[A]

			joinTable foreach { table =>
				criteria.createAlias(table, table)
			}

			criteria
				.createAlias(prop, "usergroupAlias")
				.add(in("usergroupAlias.baseWebgroup", names.asJava))
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


