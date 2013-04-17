package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.reflect.BeanInfo
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.GroupService
import uk.ac.warwick.userlookup.OnCampusService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.Member
import scala.annotation.target.field
import uk.ac.warwick.userlookup.UserLookupAdapter

trait UserLookupService extends UserLookupInterface

class UserLookupServiceImpl(d: UserLookupService) extends UserLookupServiceAdapter(d) with UserLookupService with Daoisms {

	override def getUserByWarwickUniId(id: String) =
		getUserByWarwickUniId(id, true)

	/**
	 * When looking up a user by University ID, check our internal database first.
	 */
	override def getUserByWarwickUniId(id: String, ignored: Boolean) = {
		getById[Member](id) map { member =>
			member.asSsoUser
		} getOrElse {
			super.getUserByWarwickUniId(id, ignored)
		}
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
	def clearCaches() = delegate.clearCaches()
	def getUserByIdAndPassNonLoggingIn(u: String, p: String) = delegate.getUserByIdAndPassNonLoggingIn(u, p)

}