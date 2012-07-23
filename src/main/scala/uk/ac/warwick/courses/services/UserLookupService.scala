package uk.ac.warwick.courses.services

import java.util.Map
import java.util.List
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.immutable
import scala.reflect.BeanInfo
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.GroupService
import uk.ac.warwick.userlookup.OnCampusService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.userlookup.UserLookupInterface
import scala.reflect.BeanProperty
import scala.annotation.target.field
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.UpstreamMember
import uk.ac.warwick.userlookup.UserLookupAdapter

trait UserLookupService extends UserLookupInterface

class UserLookupServiceImpl(d:UserLookupInterface) extends UserLookupAdapter(d) with UserLookupService with Daoisms {
	
	override def getUserByWarwickUniId(id:String) =
		getUserByWarwickUniId(id, true)
	
	/**
	 * When looking up a user by University ID, check our internal database first.
	 */
	override def getUserByWarwickUniId(id: String, ignored: Boolean) = {
		getById[UpstreamMember](id) map { member =>
			member.asSsoUser
		} getOrElse {
			super.getUserByWarwickUniId(id, ignored)
		}
	}
	
}

class SwappableUserLookupService(d:UserLookupService) extends UserLookupServiceAdapter(d)

abstract class UserLookupServiceAdapter(@BeanProperty var delegate:UserLookupService) extends UserLookupService {
	
	def getUsersInDepartment(d: String)= delegate.getUsersInDepartment(d)
	def getUsersInDepartmentCode(c: String)= delegate.getUsersInDepartmentCode(c)
	def getUserByToken(t: String)= delegate.getUserByToken(t)
	def getUsersByUserIds(ids: List[String])= delegate.getUsersByUserIds(ids)
	def getUserByWarwickUniId(id: String)= delegate.getUserByWarwickUniId(id)
	def getUserByWarwickUniId(id: String, ignored: Boolean)= delegate.getUserByWarwickUniId(id, ignored)
	def findUsersWithFilter(map: Map[String,String])= delegate.findUsersWithFilter(map)
	def findUsersWithFilter(map: Map[String,String], includeInactive: Boolean)= delegate.findUsersWithFilter(map, includeInactive)
	def getGroupService() = delegate.getGroupService
	def getOnCampusService() = delegate.getOnCampusService
	def getUserByUserId(id: String)= delegate.getUserByUserId(id)
	def clearCaches() = delegate.clearCaches()
	def getUserByIdAndPassNonLoggingIn(u:String,p:String) = delegate.getUserByIdAndPassNonLoggingIn(u,p)
	
}

@BeanInfo
class MaintenanceUserLookup extends UserLookupService {
	
	def getUsersInDepartment(arg0: String): List[User] = Nil

	def getUsersInDepartmentCode(arg0: String): List[User] = Nil

	def getUserByToken(arg0: String): User = new AnonymousUser()

	def getUsersByUserIds(arg0: List[String]): Map[String,User] = immutable.Map.empty[String,User]

	def getUserByWarwickUniId(arg0: String): User = new AnonymousUser()

	def getUserByWarwickUniId(arg0: String, arg1: Boolean): User = new AnonymousUser()

	def findUsersWithFilter(arg0: Map[String,String]): List[User] = Nil

	def findUsersWithFilter(arg0: Map[String,String], arg1: Boolean): List[User] = Nil

	// FIXME return an implementation
	def getGroupService(): GroupService = null
	// FIXME return an implementation
	def getOnCampusService(): OnCampusService = null

	def getUserByIdAndPassNonLoggingIn(arg0: String, arg1: String): User = new AnonymousUser()

	def getUserByUserId(arg0: String): User = new AnonymousUser()

	def clearCaches() {}
}