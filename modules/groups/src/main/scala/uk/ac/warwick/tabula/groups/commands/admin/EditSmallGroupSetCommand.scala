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
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.commands.groups.RemoveUserFromSmallGroupCommand

class EditSmallGroupSetCommand(val set: SmallGroupSet, val apparentUser:User)
	extends ModifySmallGroupSetCommand(set.module) with SmallGroupSetCommand  with NotifiesAffectedGroupMembers {

	PermissionCheck(Permissions.SmallGroups.Update, set)
	
	this.copyFrom(set)
	promisedValue = set
	val setOption = Some(set)

	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		val autoDeregister = set.module.department.autoGroupDeregistration
		
		val oldUsers = 
			if (autoDeregister) membershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Option(set.members)).toSet 
			else Set[User]()
		
		val newUsers = 
			if (autoDeregister) membershipService.determineMembershipUsers(linkedUpstreamAssessmentGroups, Option(members)).toSet
			else Set[User]()
		
		copyTo(set)
			
		// TAB-1561
		if (autoDeregister) {
			val removed = oldUsers -- newUsers
			
			removed.foreach { user => 
				set.groups.asScala.foreach { group => 
					if (group.students.includesUser(user)) {
						// Wrap this in a sub-command so that we can do auditing
						new RemoveUserFromSmallGroupCommand(user, group).apply()
					}
				}
			}
		}

		service.saveOrUpdate(set)
		set
	}

	override def describe(d: Description) = d.smallGroupSet(set).properties(
		"name" -> name,
		"format" -> format)

}