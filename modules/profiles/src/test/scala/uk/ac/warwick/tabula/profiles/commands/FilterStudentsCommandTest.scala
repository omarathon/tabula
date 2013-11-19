package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.data.model.CourseType
import scala.collection.JavaConverters._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import org.mockito.ArgumentMatcher
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.tabula.data.ScalaOrder
import org.hamcrest.Description

class FilterStudentsCommandTest extends TestBase with Mockito {
	
	trait CommandTestSupport extends ProfileServiceComponent {
		val profileService = mock[ProfileService]
	}
	
	trait Fixture {
		val department = Fixtures.department("arc", "School of Architecture")
		val subDept = Fixtures.department("arc-ug", "Architecture Undergraduates")
		subDept.parent = department
		department.children.add(subDept)
		
		val mod1 = Fixtures.module("ac101", "Introduction to Architecture")
		val mod2 = Fixtures.module("ac102", "Architecture Basics")
		val mod3 = Fixtures.module("ac901", "Postgraduate Thesis")
		
		department.modules.add(mod3)
		subDept.modules.add(mod2)
		subDept.modules.add(mod1)
		
		val route1 = Fixtures.route("a501", "Architecture BA")
		val route2 = Fixtures.route("a502", "Architecture BA with Intercalated year")
		val route3 = Fixtures.route("a9p1", "Architecture MA")
		
		department.routes.add(route3)
		subDept.routes.add(route2)
		subDept.routes.add(route1)
		
		val sprF = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
		val sprP = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")
		
		val moaFT = Fixtures.modeOfAttendance("F", "FT", "Full time")
		val moaPT = Fixtures.modeOfAttendance("P", "PT", "Part time")
	}
	
	@Test
	def bindLoadsNonWithdrawnStatuses() { new Fixture {
		val command = new FilterStudentsCommand(department) with CommandTestSupport
		
		command.profileService.allSprStatuses(department) returns (Seq(sprF, sprP))
		
		command.sprStatuses.asScala should be ('empty)
		
		command.onBind(null)
		
		command.sprStatuses.asScala should be (Seq(sprF))
	}}
	
	@Test
	def commandApplyDefaults() { new Fixture {
		val command = new FilterStudentsCommand(department) with CommandTestSupport
		command.applyInternal()
		
		val expectedRestrictions = Seq()
		
		there was one(command.profileService).findStudentsByRestrictions(
			isEq(department), 
			argThat(seqToStringMatches(expectedRestrictions)), 
			argThat(seqToStringMatches(Seq(ScalaOrder.asc("lastName"), ScalaOrder.asc("firstName")))), 
			isEq(50), 
			isEq(0)
		)
	}}
	
	@Test
	def commandApplyComplicated() { new Fixture {
		val command = new FilterStudentsCommand(department) with CommandTestSupport
		
		command.studentsPerPage = 10
		command.page = 3
	
		command.courseTypes = JArrayList(CourseType.UG, CourseType.PreSessional)
		command.routes = JArrayList(route1, route3)
		command.modesOfAttendance = JArrayList(moaFT)
		command.yearsOfStudy = JArrayList(1, 5)
		command.sprStatuses = JArrayList(sprP)
		command.modules = JArrayList(mod2, mod3)
		
		command.applyInternal()
		
		val courseTypeRestriction = new ScalaRestriction(
			Restrictions.disjunction()
				.add(Restrictions.like("course.code", "U%"))
				.add(Restrictions.like("course.code", "N%"))
		)
		courseTypeRestriction.alias("mostSignificantCourse", "studentCourseDetails")
		courseTypeRestriction.alias("studentCourseDetails.course", "course")
		
		val routeRestriction = new ScalaRestriction(Restrictions.in("studentCourseDetails.route.code", JArrayList(route1.code, route3.code)))
		routeRestriction.alias("mostSignificantCourse", "studentCourseDetails")
		
		val moaRestriction = new ScalaRestriction(Restrictions.in("studentCourseYearDetails.modeOfAttendance", JArrayList(moaFT)))
		moaRestriction.alias("mostSignificantCourse", "studentCourseDetails")
		moaRestriction.alias("studentCourseDetails.latestStudentCourseYearDetails", "studentCourseYearDetails")
		
		val yosRestriction = new ScalaRestriction(Restrictions.in("studentCourseYearDetails.yearOfStudy", JArrayList(1, 5)))
		yosRestriction.alias("mostSignificantCourse", "studentCourseDetails")
		yosRestriction.alias("studentCourseDetails.latestStudentCourseYearDetails", "studentCourseYearDetails")
		
		val sprRestriction = new ScalaRestriction(Restrictions.in("studentCourseDetails.sprStatus", JArrayList(sprP)))
		sprRestriction.alias("mostSignificantCourse", "studentCourseDetails")
		
		val modRestriction = new ScalaRestriction(Restrictions.in("moduleRegistration.module", JArrayList(mod2, mod3)))
		modRestriction.alias("mostSignificantCourse", "studentCourseDetails")
		modRestriction.alias("studentCourseDetails.moduleRegistrations", "moduleRegistration")
		
		val expectedRestrictions = Seq(
			courseTypeRestriction,
			routeRestriction,
			moaRestriction,
			yosRestriction,
			sprRestriction,
			modRestriction
		)
		
		there was one(command.profileService).findStudentsByRestrictions(
			isEq(department), 
			argThat(seqToStringMatches(expectedRestrictions)), 
			argThat(seqToStringMatches(Seq(ScalaOrder.asc("lastName"), ScalaOrder.asc("firstName")))), 
			isEq(10), 
			isEq(20)
		)
	}}
	
	@Test
	def commandApplyDefaultsWithAliasedSort() { new Fixture {
		val command = new FilterStudentsCommand(department) with CommandTestSupport
		command.sortOrder = JArrayList(Order.desc("studentCourseYearDetails.yearOfStudy"))
		
		command.applyInternal()
			
		val expectedRestrictions = Seq()
		
		val expectedOrders = Seq(
			ScalaOrder(
				Order.desc("studentCourseYearDetails.yearOfStudy"),
				"mostSignificantCourse" -> "studentCourseDetails",
				"studentCourseDetails.latestStudentCourseYearDetails" -> "studentCourseYearDetails"	
			),
			ScalaOrder.asc("lastName"), 
			ScalaOrder.asc("firstName")
		)
		
		there was one(command.profileService).findStudentsByRestrictions(
			isEq(department), 
			argThat(seqToStringMatches(expectedRestrictions)), 
			argThat(seqToStringMatches(expectedOrders)), 
			isEq(50), 
			isEq(0)
		)
	}}
	
	def seqToStringMatches[A](o: Seq[A]) = new ArgumentMatcher[Seq[A]] {
		def matches(that: Any) = that match {
			case s: Seq[_] => s.length == o.length && (o, s).zipped.forall { case (a, b) => a.toString == b.toString }
			case _ => false
		}
		
		override def describeTo(description: Description) = description.appendText(o.toString)
	}

}