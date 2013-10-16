package uk.ac.warwick.tabula.services

import org.hibernate.annotations.{AccessType, Filter, FilterDef}

import javax.persistence.{DiscriminatorValue, Entity, NamedQueries}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.{AutowiringAssignmentMembershipDaoComponent, SmallGroupDao, SmallGroupDaoComponent, UserGroupDao, UserGroupDaoComponent}
import uk.ac.warwick.tabula.data.model.{Department, Module, ModuleRegistration}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User

class SmallGroupServiceTest extends TestBase with Mockito {
	val studentGroupMembershipHelper = mock[UserGroupMembershipHelper[SmallGroup]]
	val mockUserLookup:UserLookupService = smartMock[UserLookupService]
	val dept = new Department
	val user = new User
	user.setUserId("cusdx")
	user.setWarwickId("0123456")

	mockUserLookup.getUserByUserId("cusdx") returns user

	val module = new Module("am101",dept)
	val module2:Module = new Module("cs123",dept)

	val student = Fixtures.student(userId="cusdx", department=dept)

	val groupSet = new SmallGroupSet
	groupSet.module = module
	groupSet.academicYear = new AcademicYear(2013)

	val group = new SmallGroup
	group.groupSet = groupSet
	group.students.add(user)

	val service = new AbstractSmallGroupService
		with AutowiringAssignmentMembershipDaoComponent // don't need this component, so autowiring to null is fine
		with SmallGroupMembershipHelpers
		with UserLookupComponent
		with UserGroupDaoComponent
		with SmallGroupDaoComponent
		with Logging
	{
		val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent] = null
		val groupTutorsHelper: UserGroupMembershipHelper[SmallGroup] = null
		val studentGroupHelper: UserGroupMembershipHelper[SmallGroup] = studentGroupMembershipHelper
		val smallGroupDao:SmallGroupDao = smartMock[SmallGroupDao]
		smallGroupDao.findByModuleAndYear(module, new AcademicYear(2013)) returns Seq[SmallGroup](group)
		smallGroupDao.findByModuleAndYear(module2, new AcademicYear(2013)) returns Seq[SmallGroup]()
		val userGroupDao:UserGroupDao = smartMock[UserGroupDao]
		def userLookup = mockUserLookup

	}

	@Test
	def findSmallGroupsByMemberCallsMembershipHelper() {

		val user  = new User
		val group = new SmallGroup()

		studentGroupMembershipHelper.findBy(user) returns  Seq(group)
		service.findSmallGroupsByStudent(user) should be(Seq(group))
		there was one (studentGroupMembershipHelper).findBy(user)
	}

	@Test
	def testRemoveFromSmallGroups() {
		// try removing the user from the group, but pass in a different module from that associated with the group - this should fail:
		val modreg2 = new ModuleRegistration(student.mostSignificantCourseDetails.get, module2, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
		service.removeFromSmallGroups(modreg2)
		group.students.includesUser(user) should be (true)

		// try removing the user from the group, and pass in the module associated with the group
		// - but have the departmental setting set to not allow auto group deregistration - this should fail again::
		dept.autoGroupDeregistration = false
		val modreg = new ModuleRegistration(student.mostSignificantCourseDetails.get, module, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
		service.removeFromSmallGroups(modreg)
		group.students.includesUser(user) should be (true)

		// now set the departmental setting back to the default,
		// try removing the user from the group, and pass in the module associated with the group - this should succeed:
		dept.autoGroupDeregistration = true
		service.removeFromSmallGroups(modreg)
		group.students.includesUser(user) should be (false)

	}
}



