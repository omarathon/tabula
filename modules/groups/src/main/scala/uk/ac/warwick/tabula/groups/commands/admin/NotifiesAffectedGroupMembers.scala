package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.groups.notifications.{UserRoleOnGroup, SmallGroupSetChangedNotification}
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
trait NotifiesAffectedGroupMembers extends Notifies[SmallGroupSet]{
   this:SmallGroupSetCommand =>

  val setBeforeUpdates:SmallGroupSet = set.duplicateTo(set.module)

  def hasAffectedStudentsGroups(student:User) = {
    val previousMembership = setBeforeUpdates.groups.asScala.find(_.students.users.contains(student))
    val currentMemebership = set.groups.asScala.find(_.students.users.contains(student))
    // notify if the student's group membership has changed, or
    // if the events have changed on the student's group
    (previousMembership != currentMemebership) || !previousMembership.get.hasEquivalentEventsTo(currentMemebership.get)
  }

  def hasAffectedTutorsEvents(tutor:User) = {
    // can't use group.hasEquivalentEventsTo, because there might be a change to an event which this user is not a tutor of
    // - so comparisons have to be at the event level rather than the group level
    val previousEvents = setBeforeUpdates.groups.asScala.flatMap(_.events.asScala).filter(_.tutors.users.contains(tutor))
    val currentEvents = set.groups.asScala.flatMap(_.events.asScala).filter(_.tutors.users.contains(tutor))
    val noEventsRemoved = previousEvents.forall(pe=>currentEvents.exists(_.isEquivalentTo(pe)))
    val noEventsAdded = currentEvents.forall(ce=>previousEvents.exists(_.isEquivalentTo(ce)))
    if (noEventsRemoved && noEventsAdded){
      // no events have changed (therefore no groups relevant to this tutor can have changed), but the allocations might have
      val previousGroups = setBeforeUpdates.groups.asScala
      val currentGroups = set.groups.asScala
      val allocationsUnchanged = previousGroups.forall(pg=>currentGroups.exists(cg=>cg == pg && cg.students.members == pg.students.members))
      !allocationsUnchanged
    }else{
      true
    }
  }

  /**
   * Filtered view of a SmallGroupSet containing only groups and events applicable to a tutor
   */
  def tutorsEvents(set: SmallGroupSet, tutor:User)={
    val clone = set.duplicateTo(set.module)
    for (group<-clone.groups.asScala){
      group.events = group.events.asScala.filter(_.tutors.users.contains(tutor)).asJava
    }
    clone.groups = clone.groups.asScala.filterNot(_.events.isEmpty).asJava
    clone
  }

  /**
   * Filtered view of a SmallGroupSet containing only the group applicable to a student.
   */
  def studentsEvents(set:SmallGroupSet,student:User)={
    val clone = set.duplicateTo(set.module)
    clone.groups = clone.groups.asScala.filter(_.students.users.contains(student)).asJava
    clone
  }

  def createTutorNotification(tutor:User):Option[Notification[SmallGroupSet]] = {
    val filteredGroupSet = tutorsEvents(set, tutor)
    createNotification(filteredGroupSet, tutor, UserRoleOnGroup.Tutor)
  }

  def createStudentNotification(student:User):Option[Notification[SmallGroupSet]] = {
    val filteredGroupset= studentsEvents(set, student)
    createNotification(filteredGroupset, student, UserRoleOnGroup.Student)
  }

  def createNotification(filteredGroupset:SmallGroupSet,user:User, role:UserRoleOnGroup) = {
    filteredGroupset.groups.asScala.toSeq match {
      case Nil=>None
      case list=>Some(new SmallGroupSetChangedNotification(filteredGroupset,apparentUser,user, role) with FreemarkerTextRenderer)
    }
  }

  def emit: Seq[Notification[SmallGroupSet]] = {
    val tutorNotifications:Seq[Notification[SmallGroupSet]] = if (set.releasedToTutors){
      val allEvents = (setBeforeUpdates.groups.asScala ++ set.groups.asScala).flatMap(g=>g.events.asScala)
      val allTutors = allEvents.flatMap(e=>e.tutors.users).distinct
      val affectedTutors = allTutors.filter(hasAffectedTutorsEvents)
      affectedTutors.flatMap(createTutorNotification)
    }else{
      Nil
    }
    val studentNotifications = if (set.releasedToStudents){
      val allStudents = (setBeforeUpdates.groups.asScala ++ set.groups.asScala).flatMap(g=>g.students.users).distinct
      val affectedStudents = allStudents.filter(hasAffectedStudentsGroups)
      affectedStudents.flatMap(createStudentNotification)
    }else{
      Nil
    }
    studentNotifications ++ tutorNotifications
  }
}
