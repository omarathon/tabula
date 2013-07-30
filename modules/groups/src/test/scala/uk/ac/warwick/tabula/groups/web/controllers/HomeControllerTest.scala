package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.{SmallGroupBuilder, SmallGroupSetBuilder}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{Manual, StudentSignUp}
import uk.ac.warwick.userlookup.{UserLookup, User}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewerRole, StudentAssignedToGroup, StudentNotAssignedToGroup}
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import scala.collection.JavaConverters._

class HomeControllerTest extends TestBase with Mockito{

	import HomeController._

	private trait Fixture{
		val userLookup = mock[UserLookupService]

		val unallocatedUser = new User
		unallocatedUser.setWarwickId("unallocated")
		unallocatedUser.setUserId("unallocated")
		userLookup.getUserByWarwickUniId("unallocated") returns unallocatedUser

		val allocatedUser = new User
		allocatedUser.setWarwickId("allocated")
		allocatedUser.setUserId("allocated")
		userLookup.getUserByWarwickUniId("allocated") returns allocatedUser


		val group1 = new SmallGroupBuilder()
			.withStudents(createUserGroup(Seq(allocatedUser)))
			.build

		val group2 = new SmallGroupBuilder().build
		val group3 = new SmallGroupBuilder().build

		val groupSet = new SmallGroupSetBuilder()
			.withAllocationMethod(StudentSignUp)
			.withMembers(createUserGroup(Seq(allocatedUser,unallocatedUser)))
			.withGroups(Seq(group1, group2))
			.build
		val groupSet2 = new SmallGroupSetBuilder()
			.withAllocationMethod(StudentSignUp)
			.withMembers(createUserGroup(Seq(allocatedUser,unallocatedUser)))
			.withGroups(Seq(group3))
			.withModule(groupSet.module)
			.build

		def createUserGroup(users:Seq[User])= {
			val ug = new UserGroup
			ug.universityIds = true // makes stubbing the userlookup calls easier as there's no bulk fetch by warwick ID
			ug.userLookup = userLookup
			users.foreach (ug.add)
			ug
		}

	}

	@Test
	def getGroupsToDisplayReturnsNilForNonSelfSignUpWithNoAllocations() { new Fixture{
		groupSet.allocationMethod= Manual
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Nil, StudentNotAssignedToGroup))
	}}

	@Test
	def getGroupsToDisplayReturnsAllocatedGroupsForNonSelfSignUpWithNoAllocations() { new Fixture{
		groupSet.allocationMethod= Manual
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Nil, StudentNotAssignedToGroup))
	}}

	@Test
	def getGroupstoDisplayReturnsAllGroupsForOpenSelfSignupWithNoAllocation(){new Fixture{
		groupSet.openForSignups = true
		groupSet.allocationMethod = StudentSignUp
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Seq(group1, group2), StudentNotAssignedToGroup))
	}}

	@Test
	def getGroupstoDisplayReturnsNilForClosedSelfSignupWithNoAllocation(){new Fixture{
		groupSet.openForSignups = false
		groupSet.allocationMethod = StudentSignUp
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Nil, StudentNotAssignedToGroup))
	}}

	@Test
	def getGroupsToDisplayReturnsAllocatedGroupForNonSelfSignUp(){new Fixture {
		groupSet.allocationMethod= Manual
		getGroupsToDisplay(groupSet, allocatedUser) should be ((Seq(group1), StudentAssignedToGroup))
	}}

	@Test
	def getGroupsToDisplayReturnsAllocatedGroupForSelfSignUp(){new Fixture {
		groupSet.allocationMethod= StudentSignUp
		getGroupsToDisplay(groupSet, allocatedUser) should be ((Seq(group1), StudentAssignedToGroup))
	}}

	@Test
	def getViewModulesRemovesModulesWithNoGroups(){new Fixture{
		// dummy getGroupsToDisplay that always returns an empty set
		def neverReturnGroups(set:SmallGroupSet):(Seq[SmallGroup],ViewerRole)  = (Nil,StudentNotAssignedToGroup )
		getViewModulesForStudent(Seq(groupSet), neverReturnGroups) should be (Nil)
	}}

	@Test
	def getViewModulesConvertsModuleToViewModule(){new Fixture{
		// dummy getGroupsToDisplay that always returns all groups in the set
		def allGroups(set:SmallGroupSet):(Seq[SmallGroup],ViewerRole)  = (set.groups.asScala,StudentNotAssignedToGroup )
		val viewModules = getViewModulesForStudent(Seq(groupSet), allGroups)
		viewModules.size should be(1)
		viewModules.head.canManageGroups should be(false)
		val viewSets = viewModules.head.setItems
		viewSets.size should be(1)
		viewSets.head.groups should be (Seq(group1, group2))
	}}

	@Test
	def getViewModulesGroupsByModule(){new Fixture{
		// dummy getGroupsToDisplay that always returns all groups in the set
		def allGroups(set:SmallGroupSet):(Seq[SmallGroup],ViewerRole)  = (set.groups.asScala,StudentNotAssignedToGroup )

		// two groupsets, both for the same module
		val viewModules = getViewModulesForStudent(Seq(groupSet, groupSet2), allGroups)
		viewModules.size should be(1)
		val viewSets = viewModules.head.setItems
		viewSets.size should be(2)
		viewSets.head.groups should be (Seq(group1, group2))
		viewSets.tail.head.groups should be (Seq(group3))
	}}


}
