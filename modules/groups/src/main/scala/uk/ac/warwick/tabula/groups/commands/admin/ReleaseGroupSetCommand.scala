package uk.ac.warwick.tabula.groups.commands.admin


import uk.ac.warwick.tabula.commands.{Appliable, Notifies, Command, Description}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.data.model.{Module, Notification}
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.groups.notifications.ReleaseSmallGroupSetsNotification
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions.Permissions

trait ReleaseSmallGroupSetCommand extends Appliable[Seq[SmallGroupSet]]{
	def describeOutcome:Option[String]
}
class ReleaseGroupSetCommandImpl(val groupsToPublish:Seq[SmallGroupSet], private val currentUser: User) extends Command[Seq[SmallGroupSet]] with Appliable[Seq[SmallGroupSet]] with Notifies[Seq[SmallGroupSet], Seq[SmallGroup]] with ReleaseSmallGroupSetCommand{

  var userLookup:UserLookupService = Wire.auto[UserLookupService]
  var groupSetsReleasedToStudents:List[SmallGroupSet] = Nil
  var groupSetsReleasedToTutors:List[SmallGroupSet] = Nil


  var notifyStudents:JBoolean = groupsToPublish match {
    case singleGroup::Nil => !singleGroup.releasedToStudents
    case _ => true
  }

  var notifyTutors:JBoolean = groupsToPublish match {
    case singleGroup::Nil => !singleGroup.releasedToTutors
    case _=> true
  }

  groupsToPublish.foreach(g=>PermissionCheck(Permissions.SmallGroups.Update, g))

  def singleGroupToPublish:SmallGroupSet = {
    groupsToPublish match {
      case h :: Nil => h
      case Nil=>throw new RuntimeException("Attempted to get first group to publish from an empty list")
      case _ => throw new RuntimeException("Attempted to get single group to publish from a list of many")
    }
  }

	def emit(sets: Seq[SmallGroupSet]): Seq[Notification[Seq[SmallGroup]]] = {
		val tutorNotifications = if (notifyTutors) {
			for (
				groupSet <- groupSetsReleasedToTutors;
				group <- groupSet.groups.asScala;
				event <- group.events.asScala;
				tutor <- event.tutors.users
			) yield new ReleaseSmallGroupSetsNotification(List(group), currentUser, tutor, isStudent = false) with FreemarkerTextRenderer
		} else {
			Nil
		}

		val studentNotifications = if (notifyStudents) {
			for (
				groupSet <- groupSetsReleasedToStudents;
				group <- groupSet.groups.asScala;
				student <- group.students.users
			) yield new ReleaseSmallGroupSetsNotification(List(group), currentUser, student, isStudent = true) with FreemarkerTextRenderer
		} else {
			Nil
		}
		
		studentNotifications ++ tutorNotifications
	}
	
	def describeOutcome():Option[String]={
		groupsToPublish match {
			case singleGroup::Nil=>{
				val tutors = Some("tutors").filter(_ => notifyTutors.booleanValue())
				val students = Some("students").filter(_=>notifyStudents.booleanValue())
				Seq(tutors,students).flatten match {
					case Nil=>None // we've selected to notify neither staff nor students.
					case list=>{
						val updatedUsers = list.mkString(" and ").capitalize
						val moduleName = singleGroup.module.code.toUpperCase
						Some(s"$updatedUsers in <strong>${singleGroup.name} for ${moduleName}</strong> have been notified")
					}
				}
			}
			case _ => None // this function is only used when releasing a single group
		}
	}

	def describe(desc:Description ){
    desc.smallGroupSetCollection(groupsToPublish)
  }
	
	def applyInternal():Seq[SmallGroupSet] = {
    groupsToPublish.foreach(groupToPublish=>{
      if (notifyStudents && !groupToPublish.releasedToStudents){
        groupToPublish.releasedToStudents = true
        groupSetsReleasedToStudents = groupToPublish :: groupSetsReleasedToStudents
      }
      if (notifyTutors && !groupToPublish.releasedToTutors){
        groupToPublish.releasedToTutors = true
        groupSetsReleasedToTutors = groupToPublish :: groupSetsReleasedToTutors
      }
    })
    groupsToPublish
  }
}

