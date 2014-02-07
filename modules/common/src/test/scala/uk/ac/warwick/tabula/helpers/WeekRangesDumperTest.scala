package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.{CurrentUser, AcademicYear, Mockito, TestBase}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, UserSettingsService, TermService}
import org.joda.time.{Interval, DateTime}
import uk.ac.warwick.tabula.data.model.{Department, UserSettings}
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.userlookup.User
import scala.util.parsing.json.JSON

class WeekRangesDumperTest extends TestBase with Mockito {


	private trait Fixture {
		val TEST_TIME = DateTime.now

		val settingsWithNumberingSystem = new UserSettings()
		settingsWithNumberingSystem.weekNumberingSystem = WeekRange.NumberingSystem.Term

		val departmentWithNumberingSystem = new Department()
		departmentWithNumberingSystem.weekNumberingSystem = WeekRange.NumberingSystem.Cumulative

		val dumper = new WeekRangesDumper with StoppedClockComponent {
			val stoppedTime = TEST_TIME
			userSettings = mock[UserSettingsService]
			when(userSettings.getByUserId("test")).thenReturn(Some(settingsWithNumberingSystem))
			termService = mock[TermService]
			departmentService = mock[ModuleAndDepartmentService]
		}

		val singleWeek = Seq((AcademicYear(2012),1,new Interval(TEST_TIME.minusWeeks(1), TEST_TIME)))
	}

	@Test
	def getsWeekRangesFromTermService() {new Fixture{ withUser("test") {
			dumper.termService.getAcademicWeeksBetween(any[DateTime],any[DateTime]) returns Nil
			dumper.userSettings.getByUserId(any[String]) returns None
			dumper.departmentService.getDepartmentByCode(null) returns None
			dumper.getWeekRangesAsJSON(null) // don't need a formatter as we're not returning any rows

			there was one(dumper.termService).getAcademicWeeksBetween(TEST_TIME.minusYears(2), TEST_TIME.plusYears(2))
	}}}

	@Test
	def usesUsersPreferredNumberingSystemIfAvailable(){new Fixture{ withUser("test") {
		dumper.termService.getAcademicWeeksBetween(any[DateTime],any[DateTime]) returns singleWeek
		dumper.userSettings.getByUserId("test") returns Some(settingsWithNumberingSystem)

		def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
			numberingSystem should be(settingsWithNumberingSystem.weekNumberingSystem)
			"test"
		}
		dumper.getWeekRangesAsJSON(formatter)
	}}}

	@Test
	def usesDepartmentNumberingSystemIfNoUserPreference(){new Fixture{
		// have to manually set up a user with a department code so we can
		// mock the call to the department service
		val u = new User("test")
		u.setDepartmentCode("XX")
		val user = new CurrentUser(u,u)
		withCurrentUser(user) {
			dumper.termService.getAcademicWeeksBetween(any[DateTime],any[DateTime]) returns singleWeek
			dumper.userSettings.getByUserId("test") returns None
			dumper.departmentService.getDepartmentByCode(any[String]) returns Some(departmentWithNumberingSystem)

			def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
				numberingSystem should be(departmentWithNumberingSystem.weekNumberingSystem)
				"test"
			}
			dumper.getWeekRangesAsJSON(formatter)
	}}}
	@Test
	def passesYearAndWeekNumberToFormatter(){new Fixture{ withUser("test") {
		dumper.termService.getAcademicWeeksBetween(any[DateTime],any[DateTime]) returns singleWeek
		dumper.userSettings.getByUserId("test") returns Some(settingsWithNumberingSystem)

		def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
			year should be (AcademicYear(2012))
			weekNumber should be(1)
			"test"
		}
		dumper.getWeekRangesAsJSON(formatter)

	}}}

	@Test
	def outputsJSONArray(){new Fixture{ withUser("test") {
		dumper.termService.getAcademicWeeksBetween(any[DateTime],any[DateTime]) returns singleWeek
		dumper.userSettings.getByUserId("test") returns Some(settingsWithNumberingSystem)

		def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
			"Term 7 Week 95" // Use a value which won't be filtered out by the vacation-filter.
		}
		// there's some JSON weirdness here I don't understand; JSON.parseFull insists that
		// identifiers be single-quoted, but freemarker escapes single quotes to &quot; when
		// it renders the JSON, thus making it invalid. So the WeekRangesDumperTag will continue
		// to use single quotes, and the test can swap them for doubles to keep s.u.p.j.JSON happy.
		val jsonString = dumper.getWeekRangesAsJSON(formatter).replaceAll("'","\"")
		println(jsonString)
		val results = JSON.parseFull(jsonString)
		results match {
			case Some(a:Seq[Map[String,Any]] @unchecked) => {
				a.length should be(1)
				a.head should be (anInstanceOf[Map[String,Any]])
				a.head("desc") should be("Term 7 Week 95")
				a.head("start") should be(TEST_TIME.minusWeeks(1).getMillis)
				a.head("end") should be(TEST_TIME.getMillis)
			}
			case _=> fail("Didn't get an array of json objects back from the dumper!")
		}
	}}}

}


