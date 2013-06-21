package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.groups.notifications.SmallGroupSetChangedNotification
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.{Notifies, Command}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService

trait SmallGroupSetCommand {
  val set:SmallGroupSet
  val apparentUser:User
  var userLookup: UserLookupService
}
trait NotifiesAffectedGroupMembers extends Notifies[SmallGroup]{
   this:SmallGroupSetCommand =>

  val setBeforeUpdates:SmallGroupSet = set.duplicateTo(set.module)

  def hasAffectedStudentsGroups(studentId:String) = {
    val previousMembership = setBeforeUpdates.groups.asScala.find(_.students.members.contains(studentId))
    val currentMemebership = set.groups.asScala.find(_.students.members.contains(studentId))
    // notify if the student's group membership has changed, or
    // if the events have changed on the student's group
    (previousMembership != currentMemebership) || !previousMembership.get.hasEquivalentEventsTo(currentMemebership.get)

  }

  def createNotification(studentId:String):Option[Notification[SmallGroup]] = {
    val group = set.groups.asScala.find(_.students.members.contains(studentId))
    group.map(g=>new SmallGroupSetChangedNotification(g,apparentUser,userLookup.getUserByWarwickUniId(studentId)) with FreemarkerTextRenderer)
  }

  def emit: Seq[Notification[SmallGroup]] = {
    if (set.releasedToStudents){
      val allStudents = (setBeforeUpdates.groups.asScala ++ set.groups.asScala).flatMap(g=>g.students.members).distinct
      val affectedStudents = allStudents.filter(hasAffectedStudentsGroups)
      affectedStudents.flatMap(createNotification)
    }else{
      Nil
    }
  }
}
