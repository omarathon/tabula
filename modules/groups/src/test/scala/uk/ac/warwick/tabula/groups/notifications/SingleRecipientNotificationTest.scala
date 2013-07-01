package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.groups.SmallGroupFixture
import junit.framework.Test
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.userlookup.User

class SingleRecipientNotificationTest extends TestBase{


  def createNotification(user:User)={
    new SingleRecipientNotification {
      val recipient: User = user
    }
  }
  @Test
  def recipientsContainsSingleUser():Unit  = new SmallGroupFixture{
    val n = createNotification(recipient)
    n.recipients should be (Seq(recipient))
  }

}
