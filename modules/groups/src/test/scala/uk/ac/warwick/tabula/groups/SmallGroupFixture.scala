package uk.ac.warwick.tabula.groups

import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, UserGroup, Module}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import org.joda.time.LocalTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{Mockito}
import uk.ac.warwick.tabula.services.UserLookupService
import org.mockito.Mockito.when

trait SmallGroupFixture extends Mockito {


  val requestingUser = new User
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

  val actor = new User
  val recipient = new User
  recipient.setWarwickId("recipient")
  val department = new Department

  val (group1,groupSet1) = createGroupSet("A Groupset 1","small group 1",SmallGroupFormat.Lab, "la101")
  val (group2,groupSet2) = createGroupSet("A Groupset 2","small group 2",SmallGroupFormat.Seminar, "la102")
  val (group3,groupSet3) = createGroupSet("A Groupset 3","small group 3",SmallGroupFormat.Tutorial, "la103")
  val (group4,groupSet4) = createGroupSet("A Groupset 4","small group 4",SmallGroupFormat.Tutorial, "la104")
  val (group5,groupSet5) = createGroupSet("A Groupset 5","small group 5",SmallGroupFormat.Lab, "la105")



  def createGroupSet(groupSetName:String, groupName:String, format: SmallGroupFormat, moduleCode:String):(SmallGroup, SmallGroupSet) = {
    val gs = new SmallGroupSet
    gs.name =groupSetName
    gs.format = format

    val mod = new Module
    mod.code=moduleCode
    mod.name="Test module " + moduleCode
    mod.department = department
    mod.groupSets = Seq(gs).asJava
    gs.module = mod



    val groupUsers = new UserGroup
    groupUsers.addUser(student1.getWarwickId)
    groupUsers.addUser(student2.getWarwickId)
    val smallGroup = new SmallGroup
    smallGroup.students = groupUsers
    gs.groups = JList(smallGroup)
    smallGroup.groupSet = gs
    smallGroup.name = groupName

    val event = new SmallGroupEvent()
    event.startTime = new LocalTime(12,0,0,0)
    event.day = DayOfWeek.Monday
    event.location = "CMR0.1"
    event.tutors = new UserGroup
    event.tutors.addUser(tutor1.getWarwickId)
    event.tutors.addUser(tutor2.getWarwickId)
    smallGroup.events.add(event)
    (smallGroup, gs)
  }
}
