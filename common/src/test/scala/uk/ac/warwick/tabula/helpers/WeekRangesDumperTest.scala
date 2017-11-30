package uk.ac.warwick.tabula.helpers

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.{Department, UserSettings}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.util.parsing.json.JSON

class WeekRangesDumperTest extends TestBase with Mockito {

	private trait Fixture {
		val TEST_TIME: DateTime = new DateTime(2013, DateTimeConstants.NOVEMBER, 1, 9, 50, 19)

		val settingsWithNumberingSystem = new UserSettings()
		settingsWithNumberingSystem.weekNumberingSystem = WeekRange.NumberingSystem.Term

		val departmentWithNumberingSystem = new Department()
		departmentWithNumberingSystem.weekNumberingSystem = WeekRange.NumberingSystem.Cumulative

		val dumper = new WeekRangesDumper with StoppedClockComponent with UserSettingsServiceComponent with ModuleAndDepartmentServiceComponent {
			val stoppedTime = TEST_TIME
			val userSettingsService: UserSettingsService = mock[UserSettingsService]
			val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
		}
	}

	@Test
	def usesUsersPreferredNumberingSystemIfAvailable(){new Fixture{ withUser("test") { withFakeTime(TEST_TIME) {
		dumper.userSettingsService.getByUserId("test") returns Some(settingsWithNumberingSystem)

		def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
			numberingSystem should be(settingsWithNumberingSystem.weekNumberingSystem)
			"test"
		}
		dumper.getWeekRangesAsJSON(formatter)
	}}}}

	@Test
	def usesDepartmentNumberingSystemIfNoUserPreference(){new Fixture{ withFakeTime(TEST_TIME) {
		// have to manually set up a user with a department code so we can
		// mock the call to the department service
		val u = new User("test")
		u.setDepartmentCode("XX")
		val user = new CurrentUser(u,u)
		withCurrentUser(user) {
			dumper.userSettingsService.getByUserId("test") returns None
			dumper.moduleAndDepartmentService.getDepartmentByCode(any[String]) returns Some(departmentWithNumberingSystem)

			def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
				numberingSystem should be(departmentWithNumberingSystem.weekNumberingSystem)
				"test"
			}
			dumper.getWeekRangesAsJSON(formatter)
	}}}}

	@Test
	def passesYearAndWeekNumberToFormatter(){new Fixture{ withUser("test") { withFakeTime(TEST_TIME) {
		dumper.userSettingsService.getByUserId("test") returns Some(settingsWithNumberingSystem)

		var first = true
		def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
			if (first) {
				year should be(AcademicYear(2011))
				weekNumber should be(5)
				first = false
			}
			"test"
		}
		dumper.getWeekRangesAsJSON(formatter)
	}}}}

	@Test
	def outputsJSONArray(){new Fixture { withUser("test") { withFakeTime(TEST_TIME) {
		dumper.userSettingsService.getByUserId("test") returns Some(settingsWithNumberingSystem)

		def formatter(year: AcademicYear, weekNumber: Int, numberingSystem: String) = {
			"Term 7 Week 95" // Use a value which won't be filtered out by the vacation-filter.
		}
		// there's some JSON weirdness here I don't understand; JSON.parseFull insists that
		// identifiers be single-quoted, but freemarker escapes single quotes to &quot; when
		// it renders the JSON, thus making it invalid. So the WeekRangesDumperTag will continue
		// to use single quotes, and the test can swap them for doubles to keep s.u.p.j.JSON happy.
		val jsonString = dumper.getWeekRangesAsJSON(formatter).replaceAll("'","\"")
		val results = JSON.parseFull(jsonString)
		results match {
			case Some(a:Seq[Map[String,Any]] @unchecked) => {
				a.length should be(146)
				a.head should be (anInstanceOf[Map[String,Any]])
				a.head("desc") should be("Term 7 Week 95")
				a.head("start") should be(new DateTime(2011, DateTimeConstants.NOVEMBER, 7, 0, 0, 0).minusWeeks(1).getMillis)
				a.head("end") should be(new DateTime(2011, DateTimeConstants.NOVEMBER, 7, 0, 0, 0).getMillis)
			}
			case _=> fail("Didn't get an array of json objects back from the dumper!")
		}
	}}}}

}



