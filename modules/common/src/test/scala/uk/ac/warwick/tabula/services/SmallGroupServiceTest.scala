package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, Module, ModuleRegistration}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class SmallGroupServiceTest extends TestBase with Mockito {
	trait Environment {
		val studentGroupMembershipHelper = smartMock[UserGroupMembershipHelper[SmallGroup]]
		val departmentStudentGroupMembershipHelper = smartMock[UserGroupMembershipHelper[DepartmentSmallGroup]]

		val mockUserLookup: UserLookupService = smartMock[UserLookupService]

		val dept = new Department

		val user = new User
		user.setUserId("cusdx")
		user.setWarwickId("0123456")

		val module = new Module("am101", dept)
		val module2: Module = new Module("cs123", dept)

		val student = Fixtures.student(userId = "cusdx", department = dept)

		val groupSet = new SmallGroupSet
		groupSet.module = module
		groupSet.academicYear = new AcademicYear(2013)

		val groupSet2 = new SmallGroupSet
		groupSet.module = module2
		groupSet.academicYear = new AcademicYear(2013)

		val group = new SmallGroup
		group.groupSet = groupSet
		group.students.add(user)

		val group2 = new SmallGroup
		group2.groupSet = groupSet2
		group2.students.add(user)

		val modreg = new ModuleRegistration(student.mostSignificantCourseDetails.get, module, new JBigDecimal(30), new AcademicYear(2013), "A")
		val modreg2 = new ModuleRegistration(student.mostSignificantCourseDetails.get, module2, new JBigDecimal(30), new AcademicYear(2013), "A")

		val service = new AbstractSmallGroupService
			with AutowiringAssessmentMembershipDaoComponent // don't need this component, so autowiring to null is fine
			with SmallGroupMembershipHelpers
			with UserLookupComponent
			with UserGroupDaoComponent
			with SmallGroupDaoComponent
		  with SecurityServiceComponent
			with TermServiceComponent
			with Logging {
				val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent] = null
				val groupSetManualMembersHelper: UserGroupMembershipHelper[SmallGroupSet] = null
				val departmentGroupSetManualMembersHelper: UserGroupMembershipHelper[DepartmentSmallGroupSet] = null
				val studentGroupHelper: UserGroupMembershipHelper[SmallGroup] = studentGroupMembershipHelper
				val departmentStudentGroupHelper: UserGroupMembershipHelper[DepartmentSmallGroup] = departmentStudentGroupMembershipHelper
				val termService = null

				val smallGroupDao: SmallGroupDao = smartMock[SmallGroupDao]
				smallGroupDao.findByModuleAndYear(module, new AcademicYear(2013)) returns Seq[SmallGroup](group)
				smallGroupDao.findByModuleAndYear(module2, new AcademicYear(2013)) returns Seq[SmallGroup]()

				val userGroupDao: UserGroupDao = smartMock[UserGroupDao]
			  val securityService: SecurityService = smartMock[SecurityService]
				def userLookup = mockUserLookup
		}
	}

	@Test
	def findSmallGroupsByMemberCallsMembershipHelper() {
		new Environment {
			val departmentGroup = new DepartmentSmallGroup(new DepartmentSmallGroupSet)
			departmentGroup.linkedGroups.add(group2)

			studentGroupMembershipHelper.findBy(user) returns Seq(group)
			departmentStudentGroupMembershipHelper.findBy(user) returns Seq(departmentGroup)

			service.findSmallGroupsByStudent(user) should be(Seq(group, group2))
			verify(studentGroupMembershipHelper, times(1)).findBy(user)
			verify(departmentStudentGroupMembershipHelper, times(1)).findBy(user)
		}
	}

	@Test
	def testRemoveFromSmallGroups() {
		new Environment {
			mockUserLookup.getUserByUserId("cusdx") returns user

			// try removing the user from the group, but pass in a different module from that associated with the group - this should fail:
			service.removeFromSmallGroups(modreg2)
			group.students.includesUser(user) should be (true)

			// try removing the user from the group, and pass in the module associated with the group
			// - but have the departmental setting set to not allow auto group deregistration - this should fail again::
			dept.autoGroupDeregistration = false
			service.removeFromSmallGroups(modreg)
			group.students.includesUser(user) should be (true)

			// now set the departmental setting back to the default,
			// try removing the user from the group, and pass in the module associated with the group - this should succeed:
			dept.autoGroupDeregistration = true
			service.removeFromSmallGroups(modreg)
			group.students.includesUser(user) should be (false)

			// now check the user is still in group2:
			group2.students.includesUser(user) should be (true)
		}
	}
}



