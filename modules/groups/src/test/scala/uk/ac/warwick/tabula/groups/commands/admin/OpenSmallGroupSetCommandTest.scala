package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.{Notifies, Appliable, UserAware, Description}
import uk.ac.warwick.tabula.data.model.{Notification, UserGroup}
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.tabula.services.UserLookupService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpState

class OpenSmallGroupSetCommandTest extends TestBase with Mockito {

	val operator = mock[User]

	@Test
	def marksSelfSignupGroupAsOpen() {
		val set = new SmallGroupSet()
		set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
		val cmd = new OpenSmallGroupSet(Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
		cmd.applyInternal()
		set.openForSignups should be (true)
	}

	@Test
	def ignoresSetsThatAreNotSelfSignUp() {
		val set = new SmallGroupSet()
		set.allocationMethod = SmallGroupAllocationMethod.Manual
		val cmd = new OpenSmallGroupSet(Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
		cmd.applyInternal()
		set.openForSignups should be (false)
	}


	@Test
	def processesMultipleSets() {

		val set1 = new SmallGroupSet()
		set1.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
		val set2 = new SmallGroupSet()
		set2.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

		val cmd = new OpenSmallGroupSet(Seq(set1,set2), operator, SmallGroupSetSelfSignUpState.Open)
		cmd.applyInternal()
		set1.openForSignups should be (true)
		set2.openForSignups should be (true)
	}

	@Test
	def returnsUpdatedSets() {
		val set = new SmallGroupSet()
		set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

		val cmd = new OpenSmallGroupSet(Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
		cmd.applyInternal() should be(Seq(set))
	}

	@Test
	def ignoresSetsAlreadyOpened() {
		val set = new SmallGroupSet()
		set.openForSignups = true
		set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

		val cmd = new OpenSmallGroupSet(Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
		cmd.applyInternal() should be(Nil)

	}

	@Test
	def requiresUpdatePermissionsOnAllSetsToBeOpened() {
		val set1 = new SmallGroupSet()
		val set2 = new SmallGroupSet()

		val perms = new OpenSmallGroupSetPermissions with OpenSmallGroupSetState {
			val applicableSets = Seq(set1,set2)
		}

		val checker = mock[PermissionsChecking]
		perms.permissionsCheck(checker)
		there was one(checker).PermissionCheck(Permissions.SmallGroups.Update, set1)
		there was one(checker).PermissionCheck(Permissions.SmallGroups.Update, set2)
	}

	@Test
	def auditsLogsTheGroupsetsToBeOpened() {
		val sets = Seq(new SmallGroupSet())
		val audit = new OpenSmallGroupSetAudit with OpenSmallGroupSetState {
			val eventName: String = ""
			val applicableSets: Seq[SmallGroupSet] = sets
		}
		val description = mock[Description]
		audit.describe(description)
		there was one(description).smallGroupSetCollection(sets)
	}

	trait NotificationFixture {
		// n.b. both the userID and the warwickID need to be set. The userID is used as the key for equals() operations, and
		// the warwickID is used as the key in UserGroup.members
		val student1 = new User("student1")
		student1.setWarwickId("student1")
		val student2 = new User("student2")
		student2.setWarwickId("student2")
		val student3 = new User("student3")
		student3.setWarwickId("student3")
    val students = Seq(student1,student2,student3)

		val userLookup = mock[UserLookupService]
		userLookup.getUserByWarwickUniId(any[String]) answers{id=>
			students.find(_.getWarwickId == id).getOrElse(new AnonymousUser)
		}
	}

	@Test
	def notifiesEachAffectedUser() { new NotificationFixture {

		val set1 = new SmallGroupSet()
		set1._membersGroup.includeUsers = Seq(student1.getWarwickId,student2.getWarwickId).asJava
		set1._membersGroup.userLookup = userLookup

		val s1 = set1.members.users

		val set2 = new SmallGroupSet()
		set2._membersGroup.includeUsers = Seq(student2.getWarwickId,student3.getWarwickId).asJava
		set2._membersGroup.userLookup = userLookup

		val s2 = set2.members.users

		val notifier = new OpenSmallGroupSetNotifier with OpenSmallGroupSetState with UserAware {
			val applicableSets: Seq[SmallGroupSet] = Seq(set1,set2)
			val user: User = operator
		}

		val notifications:Seq[Notification[Seq[SmallGroupSet]]]  = notifier.emit(Seq(set1,set2))

		notifications.size should be(3)
		notifications.find(_.recipients.head == student1) should be('defined)
		notifications.find(_.recipients.head == student2) should be('defined)
		notifications.find(_.recipients.head == student3) should be('defined)

	}	}

	@Test
	def CommandStateReportsFirstGroupset {
		val set = new SmallGroupSet
		val oneItem = new OpenSmallGroupSetState {
			val applicableSets: Seq[SmallGroupSet] = Seq(set)
		}
		oneItem.singleSetToOpen should be(set)

		// arguably, this could throw an IllegalStateException to warn you
		// that you're using a command with multiple sets in a context that only
		// expects there to be one.
		val twoItems= new OpenSmallGroupSetState {
			val applicableSets: Seq[SmallGroupSet] = Seq(set)
		}
		twoItems.singleSetToOpen should be(set)
	}

	@Test(expected= classOf[RuntimeException])
	def CommandStateThrowsExceptionIfAskedForSingleSetFromNone {
		val emptyList = new OpenSmallGroupSetState {
			val applicableSets: Seq[SmallGroupSet] = Seq()
		}
		emptyList.singleSetToOpen

	}

	@Test
	def openSmallGroupCommandGluesEverythingTogether() {
		val command = OpenSmallGroupSetCommand(Seq(new SmallGroupSet), operator, SmallGroupSetSelfSignUpState.Open)

		command should be (anInstanceOf[Appliable[SmallGroupSet]])
		command should be (anInstanceOf[Notifies[Seq[SmallGroupSet], Seq[SmallGroupSet]]])
		command should be (anInstanceOf[OpenSmallGroupSet])
		command should be (anInstanceOf[OpenSmallGroupSetAudit])
		command should be (anInstanceOf[OpenSmallGroupSetNotifier])
		command should be (anInstanceOf[OpenSmallGroupSetPermissions])

	}


}
