package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup._
import uk.ac.warwick.util.cache._
import org.joda.time.DateTime
import javax.annotation.PreDestroy
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.services.UserLookupService._

object UserLookupService {
	type UniversityId = String
}

trait UserLookupComponent {
	def userLookup: UserLookupService
}

trait AutowiringUserLookupComponent extends UserLookupComponent {
	var userLookup = Wire[UserLookupService]
}

trait UserLookupService extends UserLookupInterface {
	def getUserByWarwickUniIdUncached(id: UniversityId): User
	
	/**
	 * Takes a List of universityIds, and returns a Map that maps universityIds to Users. Users found
	 * in the local cache will be taken from there (and not searched for), and all other
	 * users will be searched for and entered into the cache.
	 * 
	 * All universityIds will be returned in the Map, but ones that weren't found will map to
	 * AnonymousUser objects.
	 * 
	 * @param ids Seq[UniversityId]
	 * @return Map[UniversityId, User]
	 */
	def getUsersByWarwickUniIds(ids: Seq[UniversityId]): Map[UniversityId, User]
	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId]): Map[UniversityId, User]
}

class UserLookupServiceImpl(d: UserLookupInterface) extends UserLookupAdapter(d) with UserLookupService with UserByWarwickIdCache {
	
	var profileService = Wire[ProfileService]

	override def getUserByUserId(id: String) = filterApplicantUsers(super.getUserByUserId(id))

	override def getUserByWarwickUniId(id: UniversityId) =
		getUserByWarwickUniId(id, true)

	override def getUserByWarwickUniId(id: UniversityId, ignored: Boolean) =
		UserByWarwickIdCache.get(id)
		
	override def getUsersByWarwickUniIds(ids: Seq[UniversityId]) =
		UserByWarwickIdCache.get(ids.asJava).asScala.toMap

	def getUserByWarwickUniIdUncached(id: UniversityId) = 
		profileService.getMemberByUniversityIdStaleOrFresh(id)
			.map { _.asSsoUser }
			.getOrElse { filterApplicantUsers(super.getUserByWarwickUniId(id)) }
	
	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId]) = {
		val dbUsers = profileService.getAllMembersWithUniversityIdsStaleOrFresh(ids).map { m => m.universityId -> m.asSsoUser }.toMap
		val others = (ids.diff(dbUsers.keys.toSeq)).par.map { id => 
			id -> filterApplicantUsers(super.getUserByWarwickUniId(id))
		}.toMap
		
		dbUsers ++ others
	}

	private def filterApplicantUsers(user: User) = user.getExtraProperty("urn:websignon:usertype") match {
		case "Applicant" => {
			val result = new AnonymousUser()
			result.setWarwickId(user.getWarwickId)
			result
		}
		case _ => user
	}

}

trait UserByWarwickIdCache extends CacheEntryFactory[UniversityId, User] { self: UserLookupAdapter =>
	final val UserByWarwickIdCacheName = "UserByWarwickIdCache"
	final val UserByWarwickIdCacheMaxAgeSecs = 60 * 60 * 24 // 1 day
	final val UserByWarwickIdCacheMaxSize = 100000

	final val UserByWarwickIdCache = Caches.newCache(UserByWarwickIdCacheName, this, UserByWarwickIdCacheMaxAgeSecs)
	UserByWarwickIdCache.setAsynchronousUpdateEnabled(true)
	UserByWarwickIdCache.setMaxSize(UserByWarwickIdCacheMaxSize)
	
	def getUserByWarwickUniIdUncached(id: UniversityId): User
	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId]): Map[UniversityId, User]

	def create(warwickId: UniversityId) = {
		try {
			getUserByWarwickUniIdUncached(warwickId)
		} catch {
			case e: Exception => throw new CacheEntryUpdateException(e)
		}
	}

	def shouldBeCached(user: User) = user.isVerified && user.isFoundUser // TAB-1734 don't cache not found users

	def create(warwickIds: JList[UniversityId]): JMap[UniversityId, User] = {
		try {
			getUsersByWarwickUniIdsUncached(warwickIds.asScala).asJava
		} catch {
			case e: Exception => throw new CacheEntryUpdateException(e)
		}
	}
	override def isSupportsMultiLookups() = true

	@PreDestroy
	def shutdownCache() {
		UserByWarwickIdCache.shutdown()
	}
}

class SandboxUserLookup(d: UserLookupInterface) extends UserLookupAdapter(d) {
	var profileService = Wire[ProfileService]

	private def sandboxUser(member: Member) = {
		val ssoUser = new User(member.userId)
		ssoUser.setFoundUser(true)
		ssoUser.setVerified(true)
		ssoUser.setDepartment(member.homeDepartment.name)
		ssoUser.setDepartmentCode(member.homeDepartment.code)
		ssoUser.setEmail(member.email)
		ssoUser.setFirstName(member.firstName)
		ssoUser.setLastName(member.lastName)

		member.userType match {
			case MemberUserType.Student => ssoUser.setStudent(true)
			case _ => ssoUser.setStaff(true)
		}

		ssoUser.setWarwickId(member.universityId)

		ssoUser
	}

	override def getUsersInDepartment(d: String) =
		SandboxData.Departments.find { case (code, department) => department.name == d } match {
			case Some((code, department)) => getUsersInDepartmentCode(code)
			case _ => super.getUsersInDepartment(d)
		}

	override def getUsersInDepartmentCode(c: String) =
		SandboxData.Departments.get(c) match {
			case Some(department) => {
				val students = department.routes.values.flatMap { route =>
					(route.studentsStartId to route.studentsEndId).flatMap { uniId =>
						profileService.getMemberByUniversityId(uniId.toString) map { sandboxUser(_) }
					}
				}

				val staff = (department.staffStartId to department.staffEndId).flatMap { uniId =>
					profileService.getMemberByUniversityId(uniId.toString) map { sandboxUser(_) }
				}

				(students ++ staff).toSeq.asJava
			}
			case _ => super.getUsersInDepartmentCode(c)
		}

	override def getUsersByUserIds(ids: JList[String]) =
		ids.asScala.map { userId => (userId, getUserByUserId(userId)) }.toMap.asJava

	override def getUserByUserId(id: String) =
		profileService.getAllMembersWithUserId(id, true).headOption.map { sandboxUser(_) }.getOrElse { super.getUserByUserId(id) }

	override def getUserByWarwickUniId(id: String) =
		profileService.getMemberByUniversityId(id).map { sandboxUser(_) }.getOrElse { super.getUserByUserId(id) }

	override def getUserByWarwickUniId(id: String, ignored: Boolean) = getUserByWarwickUniId(id)

}

class SwappableUserLookupService(d: UserLookupService) extends UserLookupServiceAdapter(d)

abstract class UserLookupServiceAdapter(var delegate: UserLookupService) extends UserLookupService {

	def getUsersInDepartment(d: String) = delegate.getUsersInDepartment(d)
	def getUsersInDepartmentCode(c: String) = delegate.getUsersInDepartmentCode(c)
	def getUserByToken(t: String) = delegate.getUserByToken(t)
	def getUsersByUserIds(ids: JList[String]) = delegate.getUsersByUserIds(ids)
	def getUserByWarwickUniId(id: UniversityId) = delegate.getUserByWarwickUniId(id)
	def getUserByWarwickUniId(id: UniversityId, ignored: Boolean) = delegate.getUserByWarwickUniId(id, ignored)
	def getUserByWarwickUniIdUncached(id: UniversityId) = delegate.getUserByWarwickUniIdUncached(id)
	def getUsersByWarwickUniIds(ids: Seq[UniversityId]) = delegate.getUsersByWarwickUniIds(ids)
	def getUsersByWarwickUniIdsUncached(ids: Seq[UniversityId]) = delegate.getUsersByWarwickUniIdsUncached(ids)
	def findUsersWithFilter(map: JMap[String, String]) = delegate.findUsersWithFilter(map)
	def findUsersWithFilter(map: JMap[String, String], includeInactive: Boolean) = delegate.findUsersWithFilter(map, includeInactive)
	def getGroupService() = delegate.getGroupService
	def getOnCampusService() = delegate.getOnCampusService
	def getUserByUserId(id: String) = delegate.getUserByUserId(id)
	def getCaches() = delegate.getCaches()
	def clearCaches() = delegate.clearCaches()
	def getUserByIdAndPassNonLoggingIn(u: String, p: String) = delegate.getUserByIdAndPassNonLoggingIn(u, p)
	def requestClearWebGroup(webgroup: String) = delegate.requestClearWebGroup(webgroup)

}