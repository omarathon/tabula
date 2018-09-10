package uk.ac.warwick.tabula.services.timetables
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StoppedClockComponent
import uk.ac.warwick.tabula._



object ScientiaTests {
	class ManualYears() extends PropertiesContext(Map(
		"scientia.base.url" -> "https://test-timetable.example.com/xml",
		"scientia.years" -> "16/17,12/13"
	))

	class AutoYears() extends PropertiesContext(Map(
		"scientia.base.url" -> "https://test-timetable.example.com/xml"
	))
}

class ScientiaTests extends TestBase with Mockito with FunctionalContextTesting {
	import ScientiaTests._

	def createConfiguration = new ScientiaConfigurationImpl with StoppedClockComponent with FeaturesComponent {
		override val stoppedTime: DateTime = new DateTime().withDate(2018,9,10)
		override val features: Features = new FeaturesImpl
	}

	@Test def propertyYears = inContext[ManualYears] {
		createConfiguration.perYearUris should be (Seq(
			("https://test-timetable.example.com/xml1617/", AcademicYear.parse("16/17").extended),
			("https://test-timetable.example.com/xml1213/", AcademicYear.parse("12/13").extended)
		))
	}

	@Test def autoYears = inContext[AutoYears] {
		createConfiguration.perYearUris should be (Seq(
			("https://test-timetable.example.com/xml1718/", AcademicYear.parse("17/18").extended),
			("https://test-timetable.example.com/xml1819/", AcademicYear.parse("18/19").extended)
		))
	}

}
