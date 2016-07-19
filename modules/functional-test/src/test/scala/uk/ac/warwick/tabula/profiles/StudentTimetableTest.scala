package uk.ac.warwick.tabula.profiles

import com.gargoylesoftware.htmlunit.BrowserVersion
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.profiles.pages.ProfilePage
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear}

import uk.ac.warwick.tabula.web.FeaturesDriver
import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import com.gargoylesoftware.htmlunit.BrowserVersion
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import scala.util.{Failure, Success, Try}

/**
 * N.B. To run this test, you must set a system property (in tabula.properties)to tell tabula
 * to use a local proxy for the scientia timetabling service; i.e.
 *
 * scientia.base.url=https://yourhost.warwick.ac.uk/stubTimetable/
 *
 * You should _not_ need to have your IP address added to the Syllabus+ whitelist, unless you want to
 * view timetable data for real production users.
 *
 */
class StudentTimetableTest extends BrowserTest with TimetablingFixture with GivenWhenThen {

	// The default HTMLUnit JS engine throws errors about
	// "Unexpected call to method or property access" when trying to render the profiles page
	// The code that updates the calendar title seems to trigger the error, but it doesn't happen
	// in any real browsers that I've tested with.
	// Using Firefox JS emulation avoids the problem.
	override val htmlUnitBrowserVersion = BrowserVersion.FIREFOX_38

	// TODO provide the functional tests with a TermFactory so we can work out what week we're in right now,
	// and create the events in that week. Then we can verify that they actually show up on the calendar

	"A student" should "be able to view their timetable" in {

		Given("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode, FunctionalTestAcademicYear.current,singleEvent)

		And("Student1 is a member of a small group with a single event")

		addStudentToGroup(P.Student1.usercode,testGroupSetId, "Group 1")
		createSmallGroupEvent(testGroupSetId,"Test timetabling", weekRange = "47")

		When("Student1 views their profile")
		signIn as P.Student1 to Path("/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		click on linkText("Timetable")

		currentUrl should endWith (s"/profiles/view/course/${P.Student1.warwickId}_1/2014/timetable")

	}

	"A student" should "be able to request a JSON feed of timetable events" in {
		Given("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode,FunctionalTestAcademicYear.current,singleEvent)

		And("Student1 is a member of a small group with a single event")

		addStudentToGroup(P.Student1.usercode, testGroupSetId, "Group 1")
		createSmallGroupEvent(testGroupSetId, "Test timetabling", weekRange = "47")

		When("I request the lecture API for the whole year, as that student")
		val events = requestWholeYearsTimetableFeedFor(P.Student1)

		Then("I should get two events back")
		events.size should be(2)

		And("the first should be the lecture")
		val lecture = events.head
		lecture("title") should be("CS132 Computer Organisation & Architecture Lecture (L5)")

		And("the second should be the small group event")
		val smallGroup = events.last
		smallGroup("title") should be(s"XXX654 $TEST_MODULE_NAME Tutorial (Test Place)")
	}

	"A tutor" should "be able to request their tutees timetable" in {
		Given("Marker 1 is tutor to Student 1")
		createStudentRelationship(P.Student1,P.Marker1)

		And("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode,FunctionalTestAcademicYear.current,singleEvent)

		Then("Marker 1 should be able to view Student 1's timetable")
		val events = requestWholeYearsTimetableFeedFor(P.Student1, asUser = Some(P.Marker1))
		// we should be able to find the event we just created
		events.find(e=>e("title") == "CS132 Computer Organisation & Architecture Lecture (L5)") should be ('defined)
	}

	"A member of staff" should "be able to view any student's timetable" in {
		Given("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode,FunctionalTestAcademicYear.current,singleEvent)

		Then("Marker 2 should be able to view Student 1's timetable")
		val events = Try(requestWholeYearsTimetableFeedFor(P.Student1, asUser = Some(P.Marker2)))
		events match {
			case _:Failure[Seq[Map[String,Any]]]=>fail("Should be able to get timetable feed for any student")
			case _:Success[Seq[Map[String,Any]]]=> //OK
		}
	}

	val singleEvent = <Data>
		<Activities>
			<Activity>
				<name>CS132L</name>
				<description/>
				<start>09:30</start>
				<end>11:30</end>
				<weeks>47</weeks>
				<day>0</day>
				<type>LEC</type>
				<rooms>
					<room>L5</room>
				</rooms>
				<modules>
					<module>CS132</module>
				</modules>
				<staffmembers>
					<staffmember>1170047</staffmember>
				</staffmembers>
			</Activity>
			</Activities>
		</Data>

	// should you want to test weekend events...
	val twoEvents = <Data>
		<Activities>
			<Activity>
				<name>CS132L</name>
				<description/>
				<start>09:30</start>
				<end>11:30</end>
				<weeks>47</weeks>
				<day>0</day>
				<type>LEC</type>
				<rooms>
					<room>L5</room>
				</rooms>
				<modules>
					<module>CS132</module>
				</modules>
				<staffmembers>
					<staffmember>1170047</staffmember>
				</staffmembers>
			</Activity>
			<Activity>
				<name>Party!</name>
				<description/>
				<start>19:30</start>CS132L
				<end>23:30</end>
				<weeks>47</weeks>
				<day>6</day>
				<type>LEC</type>
				<rooms>
					<room>L5</room>
				</rooms>
				<modules>
					<module>Party</module>
				</modules>
				<staffmembers>
					<staffmember>1170047</staffmember>
				</staffmembers>
			</Activity>
		</Activities>
	</Data>


}
