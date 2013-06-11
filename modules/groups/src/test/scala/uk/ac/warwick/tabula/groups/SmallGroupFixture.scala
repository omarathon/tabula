package uk.ac.warwick.tabula.groups

import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, UserGroup, Module}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import org.joda.time.LocalTime
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AcademicYear}
import uk.ac.warwick.tabula.services.UserLookupService
import org.mockito.Mockito.when

trait SmallGroupFixture extends Mockito {


  val requestingUser = new User

  val groupSet = new SmallGroupSet
  groupSet.name = "A Groupset"
  groupSet.format = SmallGroupFormat.Seminar

  val module = new Module
  module.code="la101"
  module.name="Test module"
  module.department = new Department

  groupSet.module = module


  val student1 = new User
  student1.setWarwickId("student1")

  val student2 = new User
  student2.setWarwickId("student2")

  val tutor1 = new User
  tutor1.setWarwickId("tutor1")

  val tutor2 = new User
  tutor2.setWarwickId("tutor2")

  val userLookup = mock[UserLookupService]
  when(userLookup.getUserByWarwickUniId(student1.getWarwickId)).thenReturn(student1)
  when(userLookup.getUserByWarwickUniId(student2.getWarwickId)).thenReturn(student2)
  when(userLookup.getUserByWarwickUniId(tutor1.getWarwickId)).thenReturn(tutor1)
  when(userLookup.getUserByWarwickUniId(tutor2.getWarwickId)).thenReturn(tutor2)

  val group1Users = new UserGroup
  group1Users.addUser(student1.getWarwickId)
  group1Users.addUser(student2.getWarwickId)
  val group1 = new SmallGroup
  group1.students = group1Users
  groupSet.groups = JList(group1)
  group1.groupSet = groupSet
  group1.name = "small group 1"

  val event = new SmallGroupEvent()
  event.tutors = new UserGroup
  event.tutors.addUser(tutor1.getWarwickId)
  event.tutors.addUser(tutor2.getWarwickId)

  group1.events.add(event)

  val actor = new User
  val recipient = new User
  recipient.setWarwickId("recipient")
}
