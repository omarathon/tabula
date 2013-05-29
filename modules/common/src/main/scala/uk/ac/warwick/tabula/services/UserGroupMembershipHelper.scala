package uk.ac.warwick.tabula.services

import scala.reflect._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.userlookup.{User, GroupService}
import uk.ac.warwick.spring.Wire

/**
 * Experimental. Class for getting a collection of entities that contain
 * a UserGroup that holds a given user. For example, you could use
 * UserGroupMembershipHelper[SmallGroupEvent]("tutors") to access small group
 * events for which user X is a tutor.
 *
 * FIXME needs caching (like PermissionsService).
 * FIXME needs cache dirtying (like PermissionsService)
 * TODO PermissionsService ought to use this when it is fully featured
 */
class UserGroupMembershipHelper[A : ClassTag](property: String) extends Daoisms {
  val groupService = Wire[GroupService]

  val simpleEntityName = classTag[A].runtimeClass.getSimpleName
  val cacheName = s"$simpleEntityName-$property"

  val groupsByUserSql = s"""
    select distinct r
    from $simpleEntityName r
    where
      (
        r.users.universityIds = false and
        ((:userId in elements(r.$property.staticIncludeUsers)
        or :userId in elements(r.$property.includeUsers))
        and :userId not in elements(r.$property.excludeUsers))
      ) or (
        r.users.universityIds = true and
        ((:universityId in elements(r.$property.staticIncludeUsers)
        or :universityId in elements(r.$property.includeUsers))
        and :universityId not in elements(r.$property.excludeUsers))
      )
  """

  def findGroups(user: User) = {
    val groupsByUser = session.newQuery[A](groupsByUserSql)
      .setString("universityId", user.getWarwickId)
      .setString("userId", user.getUserId)
      .seq

    val webgroupNames: Seq[String] = groupService.getGroupsNamesForUser(user.getUserId).asScala

    val groupsByWebgroup = webgroupNames.flatMap { webgroupName =>
      session.newCriteria[A]
        .createAlias("users", "users")
        .add(is(s"$property.baseWebgroup", webgroupName))
        .seq
    }

    groupsByUser ++ groupsByWebgroup
  }
}
