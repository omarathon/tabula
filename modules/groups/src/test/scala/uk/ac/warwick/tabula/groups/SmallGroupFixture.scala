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
import java.util.UUID

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

    val mod = new Module
    mod.code=moduleCode
    mod.name="Test module " + moduleCode
    mod.department = department
    mod.groupSets = JArrayList()

    val groupUsers = new UserGroup
    groupUsers.addUser(student1.getWarwickId)
    groupUsers.addUser(student2.getWarwickId)

    val event = new SmallGroupEventBuilder()
      .withTutorIds(Seq(tutor1.getWarwickId,tutor2.getWarwickId))
      .withStartTime(new LocalTime(12,0,0,0))
      .withDay(DayOfWeek.Monday)
      .withLocation("CMR0.1")
      .build

    val smallGroup = new SmallGroupBuilder()
      .withStudents(groupUsers)
      .withEvents(Seq(event))
      .withGroupName(groupName)
      .build

    val gs = new SmallGroupSetBuilder()
      .withId(groupSetName)
      .withName(groupSetName)
      .withFormat(format)
      .withModule(mod)
      .withGroups(Seq(smallGroup))
      .build

    (smallGroup, gs)
  }

}

class SmallGroupSetBuilder(){
  val template = new SmallGroupSet

  def build = {
    val set = template.duplicateTo(template.module)
    if (template.module != null){
      template.module.groupSets.add(set)
    }
    set
  }
  def withGroups(groups:Seq[SmallGroup]):SmallGroupSetBuilder = {
    template.groups = groups.asJava
    groups.foreach(g=>g.groupSet = template)
    this
  }
  def withReleasedToStudents(b: Boolean): SmallGroupSetBuilder = {
    template.releasedToStudents = b
    this
  }
  def withId (id:String): SmallGroupSetBuilder  = {
    template.id = id
    this
  }
  def withName(name:String): SmallGroupSetBuilder = {
    template.name = name
    this
  }
  def withFormat(format:SmallGroupFormat): SmallGroupSetBuilder = {
    template.format = format
    this
  }
  def withModule(mod:Module): SmallGroupSetBuilder = {
    template.module = mod
    this
  }
}
class SmallGroupBuilder(){

  val template = new SmallGroup
  template.id = UUID.randomUUID.toString
  def build:SmallGroup = template.duplicateTo(template.groupSet)

  def withEvents(events: Seq[SmallGroupEvent]):SmallGroupBuilder = {
    template.events = events.asJava
    events.foreach(_.group = template)
    this
  }
  def withStudents(members:UserGroup):SmallGroupBuilder = {
    template.students = members
    this
  }
  def withStudentIds(ids:Seq[String]):SmallGroupBuilder = {
    val users = UserGroup.emptyUniversityIds
    users.includeUsers = ids.asJava
    withStudents(users)
  }
  def withGroupName(s: String) = {
    template.name = s
    this
  }

}

class SmallGroupEventBuilder(){

  val template = new SmallGroupEvent
  def build = template.duplicateTo(template.group)

  def withTutors(members:UserGroup):SmallGroupEventBuilder = {
    template.tutors = members
    this
  }

  def withTutorIds(ids:Seq[String]):SmallGroupEventBuilder = {
    val users = UserGroup.emptyUniversityIds
    users.includeUsers = ids.asJava
    withTutors(users)

  }

  def withStartTime(value: LocalTime):SmallGroupEventBuilder = {
    template.startTime = value
    this
  }

  def withDay(value: DayOfWeek):SmallGroupEventBuilder = {
    template.day = value
    this
  }

  def withLocation(s: String)  = {
    template.location = s
    this
  }





}
