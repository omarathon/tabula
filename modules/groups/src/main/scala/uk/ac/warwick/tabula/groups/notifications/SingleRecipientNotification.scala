package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.userlookup.User

trait SingleRecipientNotification {

  val recipient:User
  def recipients: Seq[User] = {
    Seq(recipient)
  }
}
