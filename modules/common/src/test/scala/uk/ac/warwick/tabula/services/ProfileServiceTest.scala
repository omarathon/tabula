package uk.ac.warwick.tabula.services


import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.junit.Before
import uk.ac.warwick.tabula.{AcademicYear, PersistenceTestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.model.{DegreeType, Department, SitsStatus, StudentCourseDetails, Route, Member}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.JavaImports._
import org.mockito.Matchers
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StaffMember

// scalastyle:off magic.number
class ProfileServiceTest extends PersistenceTestBase with Mockito {

	var profileService: ProfileService = _

	@Before def setup: Unit = transactional { tx =>
		val thisMemberDao = new MemberDaoImpl
		thisMemberDao.sessionFactory = sessionFactory
		val thisStudentCourseDetailsDao = new StudentCourseDetailsDaoImpl
		thisStudentCourseDetailsDao.sessionFactory = sessionFactory
		profileService = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao = thisMemberDao
			val studentCourseDetailsDao = thisStudentCourseDetailsDao
			val staffAssistantsHelper = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}
	}

	@Test def crud = transactional { tx =>
		val m1 = Fixtures.student(universityId = "0000001", userId="student")
		val m2 = Fixtures.student(universityId = "0000002", userId="student")

		val m3 = Fixtures.staff(universityId = "0000003", userId="staff1")
		val m4 = Fixtures.staff(universityId = "0000004", userId="staff2")

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		profileService.save(m1)
		profileService.save(m2)

		profileService.getMemberByUniversityId("0000001") should be (Some(m1))
		profileService.getMemberByUniversityId("0000002") should be (Some(m2))
		profileService.getMemberByUniversityId("0000003") should be (Some(m3))
		profileService.getMemberByUniversityId("0000004") should be (Some(m4))
		profileService.getMemberByUniversityId("0000005") should be (None)

		profileService.getStudentBySprCode("0000001/1") should be (Some(m1))
		profileService.getStudentBySprCode("0000002/1") should be (Some(m2))
		profileService.getStudentBySprCode("0000003/1") should be (None)
		profileService.getStudentBySprCode("0000001/2") should be (None) // TODO This may not be the right behaviour

		session.enableFilter(Member.StudentsOnlyFilter)

		profileService.getMemberByUniversityId("0000003") should be (None)
		profileService.getMemberByUniversityId("0000004") should be (None)

		profileService.getAllMembersWithUserId("student", disableFilter = false) should be (Seq(m1, m2))
		profileService.getMemberByUser(new User("student"), disableFilter = false) should be (Some(m1))
		profileService.getAllMembersWithUserId("student", disableFilter = true) should be (Seq(m1, m2))
		profileService.getAllMembersWithUserId("staff1", disableFilter = false) should be (Seq())
		profileService.getMemberByUser(new User("staff1"), disableFilter = false) should be (None)
		profileService.getAllMembersWithUserId("staff1", disableFilter = true) should be (Seq(m3))
		profileService.getMemberByUser(new User("staff1"), disableFilter = true) should be (Some(m3))
		profileService.getAllMembersWithUserId("unknown", disableFilter = false) should be (Seq())
		profileService.getAllMembersWithUserId("unknown", disableFilter = true) should be (Seq())

		session.disableFilter(Member.StudentsOnlyFilter)

		profileService.getAllMembersWithUserId("staff1", disableFilter = false) should be (Seq(m3))
		profileService.getMemberByUser(new User("staff1"), disableFilter = false) should be (Some(m3))
	}

	@Test def listMembersUpdatedSince = transactional { tx =>

		val dept1 = Fixtures.departmentWithId("in", "IT Services", "1")
		val dept2 = Fixtures.departmentWithId("po", "Politics", "2")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept1)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		session.flush

		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be (Seq(m1, m2, m3, m4))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(m1))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be (Seq(m2, m3, m4))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be (Seq())
	}

	@Test def getStudentsByRouteForAcademicYear = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao = mock[MemberDao]
			val studentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"

		val studentInBothYears = new StudentCourseDetails(Fixtures.student(), "studentInBothYears")
		studentInBothYears.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)
		studentInBothYears.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2013))
		)
		studentInBothYears.statusOnRoute = new SitsStatus("C")
		studentInBothYears.mostSignificant = true

		val studentInFirstYear = new StudentCourseDetails(Fixtures.student(), "studentInFirstYear")
		studentInFirstYear.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)
		studentInFirstYear.statusOnRoute = new SitsStatus("C")
		studentInFirstYear.mostSignificant = true

		val studentInSecondYear = new StudentCourseDetails(Fixtures.student(), "studentInSecondYear")
		studentInSecondYear.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2013))
		)
		studentInSecondYear.statusOnRoute = new SitsStatus("C")
		studentInSecondYear.mostSignificant = true


		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(studentInBothYears, studentInFirstYear, studentInSecondYear)

		val studentsInFirstYear = service.getStudentsByRoute(testRoute, AcademicYear(2012))
		studentsInFirstYear.size should be (2)
		studentsInFirstYear.exists(
			s => s.freshStudentCourseDetails.head.scjCode.equals(studentInSecondYear.scjCode)
		) should be (false)

		val studentsInSecondYear = service.getStudentsByRoute(testRoute, AcademicYear(2013))
		studentsInSecondYear.size should be (2)
		studentsInSecondYear.exists(
			s => s.freshStudentCourseDetails.head.scjCode.equals(studentInFirstYear.scjCode)
		) should be (false)
	}

	@Test def getStudentsByRouteWithdrawn = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao = mock[MemberDao]
			val studentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"

		val studentNoShow = new StudentCourseDetails(Fixtures.student(), "studentNoShow")
		studentNoShow.statusOnRoute = new SitsStatus("PNS")
		studentNoShow.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		val studentPermWithdrawn = new StudentCourseDetails(Fixtures.student(), "studentPermWithdrawn")
		studentPermWithdrawn.statusOnRoute = new SitsStatus("P")
		studentPermWithdrawn.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		val studentPermWithdrawnDebt = new StudentCourseDetails(Fixtures.student(), "studentPermWithdrawnDebt")
		studentPermWithdrawnDebt.statusOnRoute = new SitsStatus("PD")
		studentPermWithdrawnDebt.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		val studentPermWithdrawnWrittenOff = new StudentCourseDetails(Fixtures.student(), "studentPermWithdrawnWrittenOff")
		studentPermWithdrawnWrittenOff.statusOnRoute = new SitsStatus("PR")
		studentPermWithdrawnWrittenOff.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(
			studentNoShow, studentPermWithdrawn, studentPermWithdrawnDebt, studentPermWithdrawnWrittenOff
		)

		val studentsWithYear = service.getStudentsByRoute(testRoute, AcademicYear(2012))
		studentsWithYear.size should be (0)

		val students = service.getStudentsByRoute(testRoute)
		students.size should be (0)
	}



	@Test def getStudentsByRouteMostSignificantCourse = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao = mock[MemberDao]
			val studentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"
		val testYear = AcademicYear(2012)

		val student = new StudentCourseDetails(Fixtures.student(), "student")

		student.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)
		student.statusOnRoute = new SitsStatus("C")
		student.mostSignificant =  true

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(student)

		val students = service.getStudentsByRoute(testRoute, testYear)
		students.size should be (1)

		students.exists(
			s => s.freshStudentCourseDetails.head.mostSignificant.equals(true)
		) should be (true)

	}



	@Test def getStudentsByRouteNotMostSignificantCourse = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao = mock[MemberDao]
			val studentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"
		val testYear = AcademicYear(2012)

		val student = new StudentCourseDetails(Fixtures.student(), "student")

		student.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)
		student.statusOnRoute = new SitsStatus("C")
		student.mostSignificant = false

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(student)

		val students = service.getStudentsByRoute(testRoute, testYear)
		students.size should be (0)

		students.exists(
			s => s.freshStudentCourseDetails.head.mostSignificant.equals(true)
		) should be (false)

	}
	trait MockFixture {
		val profileServiceWithMocks = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao = mock[MemberDao]
			val studentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}
	}

	trait SubDepartmentsFixture extends MockFixture {
		val subDepartment = new Department
		subDepartment.filterRule = Department.UndergraduateFilterRule
		val otherSubDepartment = new Department
		otherSubDepartment.filterRule = Department.PostgraduateFilterRule
		val parentDepartment = new Department
		parentDepartment.children = JHashSet(subDepartment, otherSubDepartment)
		subDepartment.parent = parentDepartment
		otherSubDepartment.parent = parentDepartment
		val ugRoute = Fixtures.route("ug")
		ugRoute.degreeType = DegreeType.Undergraduate
		val pgRoute = Fixtures.route("pg")
		pgRoute.degreeType = DegreeType.Postgraduate

		val studentInSubDepartment = Fixtures.student(department=subDepartment)
		studentInSubDepartment.mostSignificantCourseDetails.get.route = ugRoute
		val studentInOtherSubDepartment = Fixtures.student(department=otherSubDepartment)
		studentInOtherSubDepartment.mostSignificantCourseDetails.get.route = pgRoute
	}

	@Test def findStudentsInSubDepartment {
		new SubDepartmentsFixture {
			profileServiceWithMocks.memberDao.findStudentsByRestrictions(
				any[Iterable[ScalaRestriction]], any[Iterable[ScalaOrder]], any[JInteger], any[JInteger]
			) returns Seq(studentInSubDepartment, studentInOtherSubDepartment)

			val (offset, students) = profileServiceWithMocks.findStudentsByRestrictions(subDepartment, Seq(), Seq(), Int.MaxValue, 0)
			offset should be (0)
			students.size should be (1)
			students.head should be (studentInSubDepartment)
		}
	}

	@Test def countStudentsInSubDepartment {
		new SubDepartmentsFixture {
			profileServiceWithMocks.memberDao.findStudentsByRestrictions(
				any[Iterable[ScalaRestriction]], any[Iterable[ScalaOrder]], any[JInteger], any[JInteger]
			) returns Seq(studentInSubDepartment, studentInOtherSubDepartment)

			val count = profileServiceWithMocks.countStudentsByRestrictions(subDepartment, Seq())
			count should be (1)
		}
	}

}
