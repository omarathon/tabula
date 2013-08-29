package uk.ac.warwick.tabula.profiles

import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.home.FeaturesDriver
import uk.ac.warwick.tabula.profiles.pages.ProfilePage

/**
 * N.B. To run this test, you must set a system property (in tabula.properties)to tell tabula
 * to use a local proxy for the scientia timetabling service; i.e.
 *
 * scientia.base.url=https://yourhost.warwick.ac.uk/scheduling/stubTimetable/student
 *
 * You should _not_ need to have your IP address added to the Syllabus+ whitelist, unless you want to
 * view timetable data for real production users.
 *
 */
class StudentTimetableTest extends BrowserTest with TimetablingFixture with  GivenWhenThen{

	// TODO provide the functional tests with a TermFactory so we can work out what week we're in right now,
	// and create the events in that week. Then we can verify that they actually show up on the calendar
	
	"A student" should "be able to view their timetable" in {

		Given("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode,singleEvent)

		And("Student1 is a member of a small group with a single event")

		addStudentToGroup(P.Student1.usercode,testGroupSetId, "Group 1")
		createSmallGroupEvent(testGroupSetId,"Test timetabling", weekRange = "47")

		When("Student1 views their profile")
		signIn as(P.Student1) to (Path("/profiles"))
		val profilePage = new ProfilePage()
		profilePage should be('currentPage)

		profilePage.timetablePane should be ('defined)
		val timetable = profilePage.timetablePane.get
		timetable should be('showingCalendar)

	}
	"A student" should "be able to request a JSON feed of timetable events" in {

		Given("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode,singleEvent)

		And("Student1 is a member of a small group with a single event")

		addStudentToGroup(P.Student1.usercode,testGroupSetId, "Group 1")
		createSmallGroupEvent(testGroupSetId,"Test timetabling", weekRange = "47")

		When("I request the lecture API for the whole year, as that student")
		val events = requestWholeYearsTimetableFeedFor(P.Student1)

		Then("I should get two events back")
		events.size should be(2)

		And("the first should be the lecture")
		val lecture = events.head
		lecture("title") should be("CS132 Lecture (L5)")

		And("the second should be the small group event")
		val smallGroup = events.last
		smallGroup("title") should be("XXX654 Tutorial (Test Place)")
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
