package uk.ac.warwick.tabula.services

import java.io.Serializable

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging, RequestLevelCache}
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.UserLookupService._
import uk.ac.warwick.userlookup.webgroups.{GroupInfo, GroupNotFoundException, GroupServiceException}
import uk.ac.warwick.userlookup._
import uk.ac.warwick.util.cache._

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object UserLookupService {
  type UniversityId = String
  type Usercode = String
}

trait UserLookupComponent {
  def userLookup: UserLookupService
}

trait AutowiringUserLookupComponent extends UserLookupComponent {
  @transient var userLookup: UserLookupService = Wire[UserLookupService]
}

trait UserLookupService extends UserLookupInterface {
  override def getGroupService: LenientGroupService

  // Deliberately doesn't use the same name as the Java equivalents to avoid confusion
  final def usersByWarwickUniIds(ids: Seq[UniversityId]): Map[UniversityId, User] = getUsersByWarwickUniIds(ids.asJava).asScala.toMap
  final def usersByUserIds(ids: Seq[String]): Map[String, User] = getUsersByUserIds(ids.asJava).asScala.toMap
}

class UserLookupServiceImpl(d: UserLookupInterface)
  extends UserLookupAdapter(d)
    with UserLookupService {

  override def getGroupService: LenientGroupService = new LenientGroupService(super.getGroupService)
}

/**
 * Only Autowired in if the <code>dev</code> Spring profile is enabled; this checks the MEMBER database
 * table as a <strong>last</strong> resort if a lookup returns an AnonymousUser; this is primarily to support the functional
 * tests and overrides in <code>membership.usercode_overrides</code>.
 */
class DatabaseAwareUserLookupService(d: UserLookupInterface) extends UserLookupAdapter(d) with AutowiringProfileServiceComponent {
  override def getUsersByUserIds(ids: JList[String]): JMap[String, User] =
    super.getUsersByUserIds(ids).asScala.map { case (userId, userFromSSO) =>
      if (userFromSSO.isFoundUser) userId -> userFromSSO
      else profileService.getAllMembersWithUserId(userId, disableFilter = true).headOption match {
        case Some(member) => userId -> member.asSsoUser
        case _ => userId -> userFromSSO
      }
    }.asJava

  override def getUsersByWarwickUniIds(warwickUniIds: JList[UniversityId]): JMap[UniversityId, User] =
    super.getUsersByWarwickUniIds(warwickUniIds).asScala.map { case (uniId, userFromSSO) =>
      if (userFromSSO.isFoundUser) uniId -> userFromSSO
      else profileService.getMemberByUniversityId(uniId) match {
        case Some(member) => uniId -> member.asSsoUser
        case _ => uniId -> userFromSSO
      }
    }.asJava

  override def getUsersByWarwickUniIds(warwickUniIds: JList[UniversityId], ignored: Boolean): JMap[UniversityId, User] =
    getUsersByWarwickUniIds(warwickUniIds)

  override def getUserByUserId(id: String): User = RequestLevelCache.cachedBy("DatabaseAwareUserLookupService.getUserByUserId", id) {
    super.getUserByUserId(id) match {
      case FoundUser(u) => u
      case u =>
        profileService.getAllMembersWithUserId(id, disableFilter = true).headOption
          .map(_.asSsoUser)
          .getOrElse(u)
    }
  }

  override def getUserByWarwickUniId(id: String): User = RequestLevelCache.cachedBy("DatabaseAwareUserLookupService.getUserByWarwickUniId", id) {
    super.getUserByWarwickUniId(id) match {
      case FoundUser(u) => u
      case u =>
        profileService.getMemberByUniversityId(id)
          .map(_.asSsoUser)
          .getOrElse(u)
    }
  }

  override def getUserByWarwickUniId(id: String, ignored: Boolean): User = getUserByWarwickUniId(id)
}

/**
 * Only autowired in if the <code>sandbox</code> Spring profile is enabled. Similar to
 * {@link DatabaseAwareUserLookupService} above, but looks in the database first rather than as a
 * last resort.
 */
class SandboxUserLookup(d: UserLookupInterface) extends DatabaseAwareUserLookupService(d) {
  override def getUsersInDepartment(d: String): JList[User] =
    SandboxData.Departments.find { case (_, department) => department.name == d } match {
      case Some((code, _)) => getUsersInDepartmentCode(code)
      case _ => super.getUsersInDepartment(d)
    }

  override def getUsersInDepartmentCode(c: String): JList[User] =
    SandboxData.Departments.get(c) match {
      case Some(department) =>
        val students = department.routes.values.flatMap { route =>
          (route.studentsStartId to route.studentsEndId).flatMap { uniId =>
            profileService.getMemberByUniversityId(uniId.toString).map(_.asSsoUser)
          }
        }

        val staff = (department.staffStartId to department.staffEndId).flatMap { uniId =>
          profileService.getMemberByUniversityId(uniId.toString).map(_.asSsoUser)
        }

        (students ++ staff).toSeq.asJava

      case _ => super.getUsersInDepartmentCode(c)
    }

  // Flip these around on the Sandbox to be database-first
  override def getUserByUserId(id: String): User = RequestLevelCache.cachedBy("SandboxUserLookup.getUserByUserId", id) {
    profileService.getAllMembersWithUserId(id, disableFilter = true).headOption
      .map(_.asSsoUser)
      .getOrElse(super.getUserByUserId(id))
  }

  override def getUserByWarwickUniId(id: String): User = RequestLevelCache.cachedBy("SandboxUserLookup.getUserByWarwickUniId", id) {
    profileService.getMemberByUniversityId(id)
      .map(_.asSsoUser)
      .getOrElse(super.getUserByWarwickUniId(id))
  }
}

class LenientGroupService(delegate: GroupService) extends GroupService with Logging {
  private def tryOrElse[A](r: => A, default: => A): A =
    Try(r) match {
      case Success(any) => any
      case Failure(e: GroupServiceException) =>
        logger.warn("Caught GroupService error", e)
        default
      case Failure(t) => throw t
    }

  def isUserInGroup(userId: String, group: String): Boolean = tryOrElse(delegate.isUserInGroup(userId, group), false)

  def getGroupInfo(name: String): GroupInfo = tryOrElse(delegate.getGroupInfo(name), throw new GroupNotFoundException(name))

  def getGroupByName(name: String): Group = tryOrElse(delegate.getGroupByName(name), throw new GroupNotFoundException(name))

  def getGroupsNamesForUser(userId: String): JList[String] = tryOrElse(delegate.getGroupsNamesForUser(userId), JArrayList())

  def getGroupsForUser(userId: String): JList[Group] = tryOrElse(delegate.getGroupsForUser(userId), JArrayList())

  def getUserCodesInGroup(group: String): JList[String] = tryOrElse(delegate.getUserCodesInGroup(group), JArrayList())

  def getGroupsForQuery(search: String): JList[Group] = tryOrElse(delegate.getGroupsForQuery(search), JArrayList())

  def getRelatedGroups(group: String): JList[Group] = tryOrElse(delegate.getRelatedGroups(group), JArrayList())

  def getGroupsForDeptCode(deptCode: String): JList[Group] = tryOrElse(delegate.getGroupsForDeptCode(deptCode), JArrayList())

  def getCaches: JMap[Usercode, JSet[Cache[_ <: Serializable, _ <: Serializable]]] = delegate.getCaches

  def clearCaches(): Unit = delegate.clearCaches()

  def setTimeoutConfig(config: WebServiceTimeoutConfig): Unit = delegate.setTimeoutConfig(config)
}
