package uk.ac.warwick.tabula.coursework.services

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
import uk.ac.warwick.tabula.coursework.data.Daoisms
import uk.ac.warwick.tabula.coursework.data.model.UpstreamMember
import uk.ac.warwick.userlookup.UserLookupAdapter

class UserLookupServiceImpl(d: UserLookupInterface) extends UserLookupAdapter(d) with uk.ac.warwick.tabula.services.UserLookupService with Daoisms {

	override def getUserByWarwickUniId(id: String) =
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