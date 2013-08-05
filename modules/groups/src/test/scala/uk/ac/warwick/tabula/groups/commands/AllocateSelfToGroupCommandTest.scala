package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.{SmallGroupSetBuilder, SmallGroupBuilder}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{StudentSignUp, Manual}

class AllocateSelfToGroupCommandTest extends TestBase with Mockito{

 	private trait Fixture{
		val testGroup = new SmallGroupBuilder().build
		val user = new User()
		user.setWarwickId("01234")
		val testGroupSet = new SmallGroupSetBuilder().withId("set1").withGroups(Seq(testGroup)).build
		val allocateCommand = new AllocateSelfToGroupCommand(user, testGroupSet)
		allocateCommand.group = testGroup

		val deallocateCommand = new DeallocateSelfFromGroupCommand(user, testGroupSet)
		deallocateCommand.group = testGroup
	}

	@Test
	def allocateAddsStudentToGroupMembers(){new Fixture{
		  allocateCommand.applyInternal()
		  testGroup.students.members should contain("01234")
	}}

	@Test
	def allocateDoesNothingIfStudentAlreadyAssigned(){new Fixture {
		testGroup.students.add(user)
		testGroup.students.members should contain("01234")

		allocateCommand.applyInternal()
		testGroup.students.members should contain("01234")
	}}

	@Test
	def allocateCommandReturnsGroupset(){new Fixture{
		allocateCommand.applyInternal() should be(testGroupSet)
	}}

	@Test
	def deallocateRemovesStudentFromGroupMembers(){new Fixture{
		testGroup.students.add(user)
		deallocateCommand.applyInternal()
		testGroup.students.members should be(Nil)
	}}

	@Test
	def deallocateDoesNothingIfStudentNotAlreadyAssigned(){new Fixture {
		deallocateCommand.applyInternal()
		testGroup.students.members should be(Nil)
	}}


	@Test
	def deallocateCommandReturnsGroupset(){new Fixture{
		deallocateCommand.applyInternal() should be(testGroupSet)
	}}

	@Test
	def requiresAllocateSelfPermission(){new Fixture {
		val perms = new StudentSignupCommandPermissions with StudentSignUpCommandState{
			val user = null
			val groupSet = testGroupSet
		}
		perms.group = testGroup
		val permissionsChecking = mock[PermissionsChecking]
		perms.permissionsCheck(permissionsChecking)
		there was one(permissionsChecking).PermissionCheck(Permissions.SmallGroups.AllocateSelf,testGroup.groupSet)
	}}

	private trait ValidatorFixture extends Fixture{
		val signUpValidator  =  new AllocateSelfToGroupValidator with StudentSignUpCommandState{
			val user: User = null
			val groupSet: SmallGroupSet = null
		}
		val unsignUpValidator  =  new DeallocateSelfFromGroupValidator with StudentSignUpCommandState{
			val user: User = null
			val groupSet: SmallGroupSet = null
		}

		signUpValidator.group = testGroup
		unsignUpValidator.group = testGroup
		val errors  = mock[Errors]
		testGroup.groupSet.allocationMethod = StudentSignUp
		testGroup.groupSet.openForSignups =true
		testGroupSet.allowSelfGroupSwitching = true



	}
	@Test
	def signUpValidatesGroupIsNotFull(){new ValidatorFixture{

		signUpValidator.validate(errors)

		there was no (errors).reject(any[String])


		testGroup.maxGroupSize = 0
		testGroupSet.defaultMaxGroupSizeEnabled = true
		testGroup should be ('full)

		signUpValidator.validate(errors)
		there was one (errors).reject("smallGroup.full")
	}}

	@Test
	def signUpValidatesGroupIsNotClosed(){new ValidatorFixture{

		signUpValidator.validate(errors)

		there was no (errors).reject(any[String])

		testGroupSet.openForSignups = false

		signUpValidator.validate(errors)
		there was one (errors).reject("smallGroup.closed")
	}}

	@Test
	def signUpValidatesAllocationMethodIsStudentSignUp(){new ValidatorFixture{

		signUpValidator.validate(errors)

		there was no (errors).reject(any[String])

		testGroupSet.allocationMethod = Manual

		signUpValidator.validate(errors)
		there was one (errors).reject("smallGroup.notStudentSignUp")
	}}

	@Test
	def unsignUpValidatesGroupAllowsSwitching(){new ValidatorFixture{

		unsignUpValidator.validate(errors)

		there was no (errors).reject(any[String])


		testGroupSet.allowSelfGroupSwitching = false

		unsignUpValidator.validate(errors)
		there was one (errors).reject("smallGroup.noSwitching")
	}}

	@Test
	def unsignUpValidatesGroupIsNotClosed(){new ValidatorFixture{

		unsignUpValidator.validate(errors)

		there was no (errors).reject(any[String])

		testGroupSet.openForSignups = false

		unsignUpValidator.validate(errors)
		there was one (errors).reject("smallGroup.closed")
	}}

	@Test
	def unsignUpValidatesAllocationMethodIsStudentSignUp(){new ValidatorFixture{

		unsignUpValidator.validate(errors)

		there was no (errors).reject(any[String])

		testGroupSet.allocationMethod = Manual

		unsignUpValidator.validate(errors)
		there was one (errors).reject("smallGroup.notStudentSignUp")
	}}



}
