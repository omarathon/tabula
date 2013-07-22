package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup._
import uk.ac.warwick.util.cache._
import org.joda.time.DateTime
import javax.annotation.PreDestroy
import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter
import scala.collection.JavaConverters._
import java.lang.String
import scala.Predef.String
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.User

trait UserLookupComponent {
	def userLookup: UserLookupService
}

trait UserLookupService extends UserLookupInterface

class UserLookupServiceImpl(d: UserLookupInterface) extends UserLookupAdapter(d) with UserLookupService with UserByWarwickIdCache {


	override def getUserByUserId(id: String) = filterApplicantUsers(super.getUserByUserId(id))

	override def getUserByWarwickUniId(id: String) =
		getUserByWarwickUniId(id, true)

	override def getUserByWarwickUniId(id: String, ignored: Boolean) =
		UserByWarwickIdCache.get(id)
		
	def getUserByWarwickUniIdUncached(id: String) = filterApplicantUsers(super.getUserByWarwickUniId(id))

	private def filterApplicantUsers(user: User) = user.getExtraProperty("urn:websignon:usertype") match {
		case "Applicant" => {
			val result = new AnonymousUser()
			result.setWarwickId(user.getWarwickId)
			result
		}
		case _ => user
	}

}

trait UserByWarwickIdCache extends CacheEntryFactory[String, User] { self: UserLookupAdapter =>
	final val UserByWarwickIdCacheName = "UserByWarwickIdCache"
	final val UserByWarwickIdCacheMaxAgeSecs = 60 * 60 * 24 // 1 day
	final val UserByWarwickIdCacheMissingAgeSecs = 60 * 60 * 2 // 2 hours
	final val UserByWarwickIdCacheMaxSize = 100000
	
	final val UserByWarwickIdCache = Caches.newCache(UserByWarwickIdCacheName, this, UserByWarwickIdCacheMaxAgeSecs)
	UserByWarwickIdCache.setAsynchronousUpdateEnabled(true)
	UserByWarwickIdCache.setMaxSize(UserByWarwickIdCacheMaxSize)
	UserByWarwickIdCache.setExpiryStrategy(new CacheExpiryStrategy[String, User]() {
		def isExpired(entry: CacheEntry[String, User]) = {
			val expires = 
				if (entry.getValue.isFoundUser) entry.getTimestamp + (UserByWarwickIdCacheMaxAgeSecs * 1000)
				else entry.getTimestamp + (UserByWarwickIdCacheMissingAgeSecs * 1000)
			
			new DateTime(expires).isBeforeNow
		}
	})
	
	def getUserByWarwickUniIdUncached(id: String): User
	
	def create(warwickId: String) =
		try {
			getUserByWarwickUniIdUncached(warwickId)
		} catch {
			case e: UserLookupException => throw new CacheEntryUpdateException(e)
		}
	
	def shouldBeCached(user: User) = user.isVerified
	
	def create(warwickIds: JList[String]): JMap[String, User] = {
		throw new UnsupportedOperationException("Multi lookups not supported")
	}
	override def isSupportsMultiLookups() = false
	
	@PreDestroy
	def shutdownCache() {
		UserByWarwickIdCache.shutdown()
	}
}

class SwappableUserLookupService(d: UserLookupService) extends UserLookupServiceAdapter(d)

abstract class UserLookupServiceAdapter(var delegate: UserLookupService) extends UserLookupService {

	def getUsersInDepartment(d: String) = delegate.getUsersInDepartment(d)
	def getUsersInDepartmentCode(c: String) = delegate.getUsersInDepartmentCode(c)
	def getUserByToken(t: String) = delegate.getUserByToken(t)
	def getUsersByUserIds(ids: JList[String]) = delegate.getUsersByUserIds(ids)
	def getUserByWarwickUniId(id: String) = delegate.getUserByWarwickUniId(id)
	def getUserByWarwickUniId(id: String, ignored: Boolean) = delegate.getUserByWarwickUniId(id, ignored)
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