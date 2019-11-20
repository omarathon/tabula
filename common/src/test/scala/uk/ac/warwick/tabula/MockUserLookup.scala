package uk.ac.warwick.tabula

import org.apache.commons.lang3.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services.{LenientGroupService, UserLookupService}
import uk.ac.warwick.userlookup._
import uk.ac.warwick.userlookup.webgroups.{GroupInfo, GroupNotFoundException}

import scala.jdk.CollectionConverters._

class MockUserLookup(var defaultFoundUser: Boolean)
  extends UserLookupAdapter(null)
    with UserLookupService
    with MockUser {

  def this() = {
    this(false)
  }

  var users: Map[String, User] = Map()
  var filterUserResult: JList[User] = JArrayList()

  var findUsersEnabled: Boolean = false

  val groupService: MockGroupService = new MockGroupService

  override def getGroupService = new LenientGroupService(groupService)

  def addFindUsersWithFilterResult(user: User) {
    filterUserResult.add(user)
  }

  override def getUserByUserId(theUserId: String): User = {
    users.getOrElse(theUserId, {
      if (defaultFoundUser) {
        val user = new User()
        user.setUserId(theUserId)
        user.setFoundUser(true)
        user
      } else {
        new AnonymousUser
      }
    })
  }

  override def getUserByWarwickUniId(warwickId: String): User =
    users.values.find(_.getWarwickId == warwickId).getOrElse {
      if (defaultFoundUser) {
        val user = new User()
        user.setWarwickId(warwickId)
        user.setFoundUser(true)
        user
      } else {
        new AnonymousUser
      }
    }

  override def getUserByWarwickUniId(warwickId: String, includeDisabledLogins: Boolean): User = getUserByWarwickUniId(warwickId)

  override def getUsersByWarwickUniIds(warwickUniIds: JList[UniversityId]): JMap[UniversityId, User] =
    getUsersByWarwickUniIds(warwickUniIds, includeDisabledLogins = true)

  override def getUsersByWarwickUniIds(warwickUniIds: JList[UniversityId], includeDisabledLogins: Boolean): JMap[UniversityId, User] =
    warwickUniIds.asScala.map { id => id -> getUserByWarwickUniId(id) }.toMap.asJava

  override def findUsersWithFilter(filterValues: JMap[String, AnyRef], numberOfResults: Int): JList[User] =
    findUsersWithFilter(filterValues, returnDisabledUsers = false, numberOfResults)

  override def findUsersWithFilter(filterValues: JMap[String, AnyRef], returnDisabledUsers: Boolean, numberOfResults: Int): JList[User] =
    if (filterValues.size() == 1 && filterValues.containsKey("warwickuniid")) {
      val uniId: String = filterValues.get("warwickuniid").asInstanceOf[String]
      JArrayList(getUserByWarwickUniId(uniId))
    } else if (findUsersEnabled) {
      val list: JList[User] = JArrayList()
      list.addAll(filterUserResult)
      list
    } else {
      throw new UnsupportedOperationException()
    }

  override def findUsersWithFilter(filterValues: JMap[String, AnyRef], returnDisabledUsers: Boolean): JList[User] = findUsersWithFilter(filterValues)

  override def getUsersByUserIds(userIds: JList[String]): JMap[String, User] =
    userIds.asScala.map { id => id -> getUserByUserId(id) }.toMap.asJava

  /**
    * Method to quickly add some mock users who exist and definitely
    * have genuine, real names.
    */
  def registerUsers(userIds: String*): Seq[User] = {
    userIds.map { id =>
      val user = mockUser(id)
      users += (id -> user)
      user
    }
  }

  def registerUserObjects(u: User*) {
    for (user <- u) {
      users += (user.getUserId -> user)
    }
  }

}

trait MockUser {
  def mockUser(id: String): User = {
    val user = new User(id)
    user.setFoundUser(true)
    user.setVerified(true)
    user.setFirstName(capitalize(id) + "y")
    user.setLastName("Mc" + capitalize(id) + "erson")
    user.setDepartment("Department of " + capitalize(id))
    user.setShortDepartment("Dept of " + capitalize(id))
    user.setDepartmentCode("D" + capitalize(id.substring(0, 1)))
    user.setEmail(id + "@example.com")
    user.setWarwickId({
      val universityId = Math.abs(id.hashCode()).toString
      if (universityId.length > 7) universityId.substring(universityId.length - 7)
      else UniversityId.zeroPad(universityId)
    })
    user
  }
}

/* Vanilla is a verified user, whose session was completely retrieved from SSO.

 * Anonymous means user is *not* logged into SSO, but *is*
 * implicitly verified - as anonymous - with default properties set,
 * including warwickId of empty string.
 *
 * Unverified means a UserLookupException has been raised, which should
 * return an UnverifiedUser, a subclass of AnonymousUser;
 * most properties should have default properties set too.
 *
 * Applicant is another special case of AnonymousUser, this time with
 * a real warwickID set.
 */
sealed abstract class UserFlavour

object UserFlavour {

  case object Anonymous extends UserFlavour

  case object Unverified extends UserFlavour

  case object Applicant extends UserFlavour

  case object Vanilla extends UserFlavour

}


class MockGroupService extends GroupService {
  var groupMap: Map[String, Group] = Map()
  var usersInGroup: Map[(String, String), Boolean] = Map() withDefaultValue false
  var groupNamesForUserMap: Map[String, Seq[String]] = Map()

  override def getGroupByName(groupName: String): Group = groupMap.getOrElse(groupName, throw new GroupNotFoundException(groupName))

  override def getGroupsForDeptCode(deptCode: String) = ???

  override def getGroupsForQuery(query: String) = ???

  override def getGroupsForUser(user: String) = ???

  override def getGroupsNamesForUser(user: String): JList[String] =
    groupNamesForUserMap.get(user) match {
      case Some(groups) => groups.asJava
      case _ => JArrayList[String]()
    }

  override def getRelatedGroups(groupName: String) = ???

  override def getUserCodesInGroup(groupName: String): _root_.uk.ac.warwick.tabula.JavaImports.JList[String] = {
    try {
      getGroupByName(groupName).getUserCodes
    } catch {
      case e: GroupNotFoundException => JList()
    }
  }

  override def isUserInGroup(user: String, group: String): Boolean = usersInGroup((user, group))

  override def setTimeoutConfig(config: WebServiceTimeoutConfig) {}

  override def getGroupInfo(name: String) = new GroupInfo(getGroupByName(name).getUserCodes.size())

  override def clearCaches() = ???

  override def getCaches() = ???

}
