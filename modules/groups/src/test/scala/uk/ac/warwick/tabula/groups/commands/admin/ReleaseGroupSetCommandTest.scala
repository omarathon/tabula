package uk.ac.warwick.tabula.groups.commands.admin
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{UserGroup, Notification}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Description, DescriptionImpl}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.services.UserLookupService

class ReleaseGroupSetCommandTest extends TestBase with Mockito {



  @Test
  def applyShouldSetReleasedToStudentsFlag() {new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
      command.notifyStudents = true
      val updatedSet = command.applyInternal()
      updatedSet.releasedToStudents.booleanValue should be(true)
   }}

  @Test
  def applyShouldNotUnsetReleasedToStudentsFlag() {new SmallGroupFixture {
    groupSet.releasedToStudents = true
    val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    command.notifyStudents = false
    val updatedSet = command.applyInternal()
    updatedSet.releasedToStudents.booleanValue should be(true)
  }}

  @Test
  def applyShouldNotSetReleasedToStudentsFlagIfNotifyStudentsIsFalse() {new SmallGroupFixture {
    groupSet.releasedToStudents = false
    val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    command.notifyStudents = false
    val updatedSet = command.applyInternal()
    updatedSet.releasedToStudents.booleanValue should be(false)
  }}

  @Test
  def applyShouldSetReleasedToTutorsFlag() {new SmallGroupFixture {
    val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    command.notifyTutors = true
    val updatedSet = command.applyInternal()
    updatedSet.releasedToTutors.booleanValue should be(true)
  }}

  @Test
  def applyShouldNotUnsetReleasedToTutorsFlag() {new SmallGroupFixture {
    groupSet.releasedToTutors = true
    val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    command.notifyTutors = false
    val updatedSet = command.applyInternal()
    updatedSet.releasedToTutors.booleanValue should be(true)
  }}

  @Test
  def applyShouldNotSetReleasedToTutorsFlagIfNotifyTutorsIsFalse() {new SmallGroupFixture {
    groupSet.releasedToTutors = false
    val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    command.notifyTutors = false
    val updatedSet = command.applyInternal()
    updatedSet.releasedToTutors.booleanValue should be(false)
  }}

  @Test
  def describeShouldIncludeSmallGroupSet() { new SmallGroupFixture{
    val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    val desc = mock[Description]
    command.describe(desc)
    verify(desc, atLeastOnce()).smallGroupSet(groupSet)
  }}


	@Test
	def emitShouldCreateNotificationToAllStudents() { new SmallGroupFixture{

    val cmd = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    cmd.notifyStudents = true
    cmd.userLookup = userLookup
		val notifications = cmd.emit
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student1"))  should be (true)
    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student2"))  should be (true)
	}}

  @Test
  def emitShouldNotCreateNotificationsIfNotifyStudentsIsFalse(){new SmallGroupFixture {
    val cmd = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    cmd.notifyStudents = false
    cmd.userLookup = userLookup
    val notifications = cmd.emit

    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student1"))  should be (false)
    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student2"))  should be (false)
  }}

  @Test
  def emitShouldCreateNotificationToAllTutors(){new SmallGroupFixture {
    val cmd = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    cmd.notifyTutors = true
    cmd.userLookup = userLookup
    val notifications = cmd.emit
    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "tutor1"))  should be (true)
    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "tutor2"))  should be (true)
  }}

  @Test
  def emitShouldNotCreateNotificationsIfNotifyTutorsIsFalse(){new SmallGroupFixture {
    val cmd = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
    cmd.notifyTutors = false
    cmd.userLookup = userLookup
    val notifications = cmd.emit

    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "tutor1"))  should be (false)
    notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "tutor2"))  should be (false)
  }}

  @Test
  def notifyStudentsIsDefaultedFromGroupSet(){new SmallGroupFixture {
    groupSet.releasedToStudents = false
    new ReleaseGroupSetCommandImpl(groupSet, requestingUser).notifyStudents.booleanValue() should be(true)

    groupSet.releasedToStudents = true
    new ReleaseGroupSetCommandImpl(groupSet, requestingUser).notifyStudents.booleanValue() should be(false)

  }}

  @Test
  def notifyTutorsIsDefaultedFromGroupSet(){new SmallGroupFixture {
    groupSet.releasedToTutors = false
    new ReleaseGroupSetCommandImpl(groupSet, requestingUser).notifyTutors.booleanValue() should be(true)

    groupSet.releasedToTutors = true
    new ReleaseGroupSetCommandImpl(groupSet, requestingUser).notifyTutors.booleanValue() should be(false)

  }}

}