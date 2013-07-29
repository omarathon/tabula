package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.groups.notifications.OpenSmallGroupSetsNotification
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer

object OpenSmallGroupSetCommand {
	def apply(setsToOpen:Seq[SmallGroupSet], user:User) = {
		new OpenSmallGroupSet(setsToOpen,user)
			with ComposableCommand[Seq[SmallGroupSet]]
			with OpenSmallGroupSetPermissions
			with OpenSmallGroupSetAudit
			with OpenSmallGroupSetNotifier
	}
}

trait OpenSmallGroupSetState {
	val openableSets:Seq[SmallGroupSet]
	// convenience value for freemarker to use when we're opening a single
	// set rather than a batch.
	def singleSetToOpen:SmallGroupSet={
		openableSets match {
			case h :: Nil => h
			case Nil=>throw new RuntimeException("Attempted to get first group to open from an empty list")
			case _ => throw new RuntimeException("Attempted to get single group to open from a list of many")
		}
	}
}
class OpenSmallGroupSet(val requestedSets: Seq[SmallGroupSet], val user: User) extends CommandInternal[Seq[SmallGroupSet]] with OpenSmallGroupSetState with UserAware {

	 val openableSets = requestedSets.filter(s => s.allocationMethod == SmallGroupAllocationMethod.StudentSignUp && !s.openForSignups)

	 def applyInternal(): Seq[SmallGroupSet] = {
		 openableSets.foreach(s => s.openForSignups = true)
		 openableSets
	 }
}

trait OpenSmallGroupSetPermissions extends RequiresPermissionsChecking {
  this: OpenSmallGroupSetState =>

	def permissionsCheck(p: PermissionsChecking) {
		openableSets.foreach(g=>p.PermissionCheck(Permissions.SmallGroups.Update, g))
	}
}

trait OpenSmallGroupSetAudit extends Describable[Seq[SmallGroupSet]] {
	this: KnowsEventName with OpenSmallGroupSetState =>
	def describe(d: Description) {
		d.smallGroupSetCollection(openableSets)
	}
}

trait OpenSmallGroupSetNotifier extends Notifies[Seq[SmallGroupSet]] {
	this: OpenSmallGroupSetState with UserAware =>

	def emit(): Seq[Notification[Seq[SmallGroupSet]]] = {
			val allMemberships: Seq[(User,SmallGroupSet)] = for (set <- openableSets;
			     member <- set.members.users) yield (member, set)

		  // convert the list of (student, set) pairs into a map of student->sets
			val setsPerUser: Map[User,Seq[SmallGroupSet]] = allMemberships.groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}

      // convert the map into a notification per user
		  setsPerUser.map {case (student, sets) => new OpenSmallGroupSetsNotification(user,student,sets) with FreemarkerTextRenderer}.toSeq

	}
}