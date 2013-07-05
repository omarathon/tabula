package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.commands.{Notifies, Description}
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.model.{Notification, Module}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.groups.notifications.SmallGroupSetChangedNotification
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer

class EditSmallGroupSetCommand(val set: SmallGroupSet, val apparentUser:User) extends  ModifySmallGroupSetCommand(set.module) with SmallGroupSetCommand  with NotifiesAffectedGroupMembers{

	val setOption = Some(set)

	PermissionCheck(Permissions.SmallGroups.Update, set)
	
	this.copyFrom(set)
	promisedValue = set

  var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {

		copyTo(set)

		service.saveOrUpdate(set)
		set
	}

	override def describe(d: Description) = d.smallGroupSet(set).properties(
		"name" -> name,
		"format" -> format)

}