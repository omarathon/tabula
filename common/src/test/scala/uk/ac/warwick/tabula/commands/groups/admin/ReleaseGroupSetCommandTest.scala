package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.notifications.groups.ReleaseSmallGroupSetsNotification
import uk.ac.warwick.tabula.{Mockito, SmallGroupEventBuilder, SmallGroupFixture, TestBase}

class ReleaseGroupSetCommandTest extends TestBase with Mockito {

  @Test
  def applyShouldSetReleasedToStudentsFlag(): Unit = {
    new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.notifyStudents = true
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      updatedSets.foreach(updatedSet =>
        updatedSet.set.releasedToStudents.booleanValue should be(true)
      )
    }
  }

  @Test
  def applyShouldNotUnsetReleasedToStudentsFlag(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToStudents = true
      groupSet2.releasedToStudents = true
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      command.notifyStudents = false
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      updatedSets.foreach(updatedSet =>
        updatedSet.set.releasedToStudents.booleanValue should be(true)
      )
    }
  }

  @Test
  def applyShouldNotSetReleasedToStudentsFlagIfNotifyStudentsIsFalse(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToStudents = false
      groupSet2.releasedToStudents = false
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.notifyStudents = false
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      updatedSets.foreach(updatedSet =>
        updatedSet.set.releasedToStudents.booleanValue should be(false)
      )
    }
  }

  @Test
  def applyShouldSetReleasedToTutorsFlag(): Unit = {
    new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.notifyTutors = true
      val updatedSet: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      updatedSets.foreach(updatedSet =>
        updatedSet.set.releasedToTutors.booleanValue should be(true)
      )
    }
  }

  @Test
  def applyShouldNotUnsetReleasedToTutorsFlag(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToTutors = true
      groupSet2.releasedToTutors = true
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.notifyTutors = false
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      updatedSets.foreach(updatedSet =>
        updatedSet.set.releasedToTutors.booleanValue should be(true)
      )
    }
  }

  @Test
  def applyShouldNotSetReleasedToTutorsFlagIfNotifyTutorsIsFalse(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToTutors = false
      groupSet2.releasedToTutors = false
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.notifyTutors = false
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      updatedSets.foreach(updatedSet =>
        updatedSet.set.releasedToTutors.booleanValue should be(false)
      )
    }
  }

  @Test
  def describeShouldIncludeSmallGroupSets(): Unit = {
    new SmallGroupFixture {
      val sets = Seq(groupSet1, groupSet2)
      val command = new ReleaseGroupSetCommandImpl(sets, requestingUser)
      val desc: Description = mock[Description]
      command.describe(desc)
      verify(desc, atLeastOnce()).smallGroupSetCollection(sets)
    }
  }


  @Test
  def emitShouldCreateNotificationToAllStudents(): Unit = {
    new SmallGroupFixture {
      val cmd = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      cmd.notifyStudents = true
      cmd.userLookup = userLookup
      cmd.applyInternal()

      val notifications: Seq[ReleaseSmallGroupSetsNotification] = cmd.emit(Seq(ReleasedSmallGroupSet(groupSet1, cmd.notifyStudents, cmd.notifyTutors)))
      notifications.foreach {
        case n: ReleaseSmallGroupSetsNotification => n.userLookup = userLookup
      }
      notifications.exists(n => n.recipients.exists(u => u.getWarwickId == "student1")) should be(true)
      notifications.exists(n => n.recipients.exists(u => u.getWarwickId == "student2")) should be(true)
    }
  }

  @Test
  def emitShouldNotCreateNotificationsIfNotifyStudentsIsFalse(): Unit = {
    new SmallGroupFixture {
      val cmd = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      cmd.notifyStudents = false
      cmd.userLookup = userLookup
      val notifications: Seq[ReleaseSmallGroupSetsNotification] = cmd.emit(Seq(ReleasedSmallGroupSet(groupSet1, cmd.notifyStudents, cmd.notifyTutors)))
      notifications.foreach {
        case n: ReleaseSmallGroupSetsNotification => n.userLookup = userLookup
      }
      notifications.exists(n => n.recipients.exists(u => u.getWarwickId == "student1")) should be(false)
      notifications.exists(n => n.recipients.exists(u => u.getWarwickId == "student2")) should be(false)
    }
  }

  @Test
  def emitShouldCreateNotificationToAllTutors(): Unit = {
    new SmallGroupFixture {
      val cmd = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      cmd.notifyTutors = true
      cmd.userLookup = userLookup
      cmd.applyInternal()
      val notifications: Seq[ReleaseSmallGroupSetsNotification] = cmd.emit(Seq(ReleasedSmallGroupSet(groupSet1, cmd.notifyStudents, cmd.notifyTutors)))
      notifications.foreach {
        case n: ReleaseSmallGroupSetsNotification => n.userLookup = userLookup
      }
      notifications.exists(n => n.recipients.exists(u => u.getUserId == "tutor1")) should be(true)
      notifications.exists(n => n.recipients.exists(u => u.getUserId == "tutor2")) should be(true)
    }
  }

  @Test
  def emitShouldCreateOneNotificationPerTutor(): Unit = {
    new SmallGroupFixture {
      val cmd = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      cmd.notifyTutors = true
      cmd.userLookup = userLookup
      cmd.applyInternal()

      groupSet1.groups.get(0).addEvent(extraEvent(day = DayOfWeek.Tuesday))

      val notifications: Seq[ReleaseSmallGroupSetsNotification] = cmd.emit(Seq(ReleasedSmallGroupSet(groupSet1, cmd.notifyStudents, cmd.notifyTutors)))
      notifications.foreach {
        case n: ReleaseSmallGroupSetsNotification => n.userLookup = userLookup
      }

      notifications.count(n => n.recipients.exists(u => u.getUserId == "tutor1")) should be(1)
      notifications.count(n => n.recipients.exists(u => u.getUserId == "tutor2")) should be(1)

    }
  }

  @Test
  def emitShouldNotCreateNotificationsIfNotifyTutorsIsFalse(): Unit = {
    new SmallGroupFixture {
      val cmd = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      cmd.notifyTutors = false
      cmd.userLookup = userLookup
      val notifications: Seq[ReleaseSmallGroupSetsNotification] = cmd.emit(Seq(ReleasedSmallGroupSet(groupSet1, cmd.notifyStudents, cmd.notifyTutors)))
      notifications.foreach {
        case n: ReleaseSmallGroupSetsNotification => n.userLookup = userLookup
      }
      notifications.exists(n => n.recipients.exists(u => u.getUserId == "tutor1")) should be(false)
      notifications.exists(n => n.recipients.exists(u => u.getUserId == "tutor2")) should be(false)
    }
  }

  @Test
  def notifyStudentsIsDefaultedFromGroupSetIfSingleGroup(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToStudents = false
      new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser).notifyStudents.booleanValue() should be(true)

      groupSet1.releasedToStudents = true
      new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser).notifyStudents.booleanValue() should be(false)

    }
  }

  @Test
  def notifyTutorsIsDefaultedFromGroupSetIfSingleGroup(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToTutors = false
      new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser).notifyTutors.booleanValue() should be(true)

      groupSet1.releasedToTutors = true
      new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser).notifyTutors.booleanValue() should be(false)

    }
  }

  @Test
  def notifyStudentsIsTrueForMultipleGroups(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToTutors = true
      new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser).notifyStudents.booleanValue() should be(true)
    }
  }

  @Test
  def notifyTutorsIsTrueForMultipleGroups(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToTutors = true
      new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser).notifyTutors.booleanValue() should be(true)
    }
  }


  @Test
  def shouldNotSendNotificationsForUnchangedGroupSets(): Unit = {
    new SmallGroupFixture {
      groupSet1.releasedToTutors = true
      groupSet1.releasedToStudents = true
      groupSet2.releasedToTutors = false
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.userLookup = userLookup
      command.notifyTutors = true
      val updatedSets: Seq[ReleasedSmallGroupSet] = command.applyInternal()
      val notifications: Seq[ReleaseSmallGroupSetsNotification] =
        command.emit(Seq(ReleasedSmallGroupSet(groupSet1, false, false), ReleasedSmallGroupSet(groupSet2, command.notifyStudents, true)))
      val groups: Seq[SmallGroup] = notifications.flatMap(_.groups)
      val allNotifiedGroupSets: Seq[SmallGroupSet] = groups.map(_.groupSet)
      allNotifiedGroupSets.exists(_ == groupSet1) should be(false)
    }
  }

  @Test(expected = classOf[RuntimeException])
  def singleGroupToPublishThrowsExceptionIfNoGroups: Unit = {
    new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(Nil, requestingUser)
      command.singleGroupToPublish
    }
  }

  @Test(expected = classOf[RuntimeException])
  def singleGroupToPublishThrowsExceptionIfMultipleGroups: Unit = {
    new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1, groupSet2), requestingUser)
      command.singleGroupToPublish
    }
  }

  @Test
  def singleGroupToPublishReturnsGroupIfExactlyOne: Unit = {
    new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      command.singleGroupToPublish should be(groupSet1)
    }
  }

  @Test
  def describeOutcomeWorks(): Unit = {
    new SmallGroupFixture {
      val command = new ReleaseGroupSetCommandImpl(Seq(groupSet1), requestingUser)
      command.notifyStudents = true
      command.notifyTutors = true
      command.describeOutcome should be(Some("Tutors and students in <strong>A Groupset 1 for LA101</strong> have been notified"))
      command.notifyStudents = true
      command.notifyTutors = false
      command.describeOutcome should be(Some("Students in <strong>A Groupset 1 for LA101</strong> have been notified"))
      command.notifyTutors = true
      command.notifyStudents = false
      command.describeOutcome should be(Some("Tutors in <strong>A Groupset 1 for LA101</strong> have been notified"))
      command.notifyTutors = false
      command.notifyStudents = false
      command.describeOutcome should be(None)

    }
  }

}
