package uk.ac.warwick.tabula.groups.commands.admin


import uk.ac.warwick.tabula.commands.{Appliable, Notifies, Command, Description}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.data.model.Notification
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.groups.notifications.ReleaseSmallGroupSetStudentNotification
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions.Permissions


class ReleaseGroupSetCommandImpl(val groupToPublish:SmallGroupSet, private val currentUser: User) extends Command[SmallGroupSet] with Appliable[SmallGroupSet] with Notifies[SmallGroup]{

  var userLookup:UserLookupService = Wire.auto[UserLookupService]

  var notifyStudents:JBoolean = true
  var notifyTutors:JBoolean = true

  PermissionCheck(Permissions.SmallGroups.Update, groupToPublish)

	def emit:Seq[Notification[SmallGroup]] =  {

   val studentNotifications = if (notifyStudents){
      groupToPublish.groups.asScala.map(group=>{
       group.students.members.map(userId=>
         new ReleaseSmallGroupSetStudentNotification(group,currentUser,userLookup.getUserByWarwickUniId(userId)) with FreemarkerTextRenderer)
      }).flatten.toSeq
    }else{
     Nil
   }
   studentNotifications

  }

	def describe(desc:Description ){
    desc.smallGroupSet(groupToPublish)
  }
	
	def applyInternal():SmallGroupSet = {
    groupToPublish.released = true
    groupToPublish
  }

}