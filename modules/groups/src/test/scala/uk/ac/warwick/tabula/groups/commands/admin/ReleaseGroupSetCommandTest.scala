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
  def applyShouldSetReleasedFlag() {new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(groupSet, requestingUser)
      val updatedSet = command.applyInternal()
      updatedSet.released.booleanValue should be(true)
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

    notifications should be(Nil)
  }}

	
}