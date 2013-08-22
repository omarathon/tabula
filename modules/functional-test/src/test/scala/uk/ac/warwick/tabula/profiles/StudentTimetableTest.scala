package uk.ac.warwick.tabula.profiles

import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.GivenWhenThen

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
class StudentTimetableTest extends BrowserTest with TimetableDriver with GivenWhenThen{
	before{
		go to (Path("/scheduling/fixtures/setup"))
	}

	val TEST_MODULE_CODE = "xxx654"
	val TEST_GROUPSET_NAME = "Timetable Test Groupset"

	"A student" should "be able to view their timetable" in {
		Given("Student1 has a membership record")
		createStudentMember(P.Student1.usercode)

		And("The timetabling service knows of a single event for student1")
		setTimetableFor(P.Student1.usercode,singleEvent)

		And("Student1 is a member of a small group with a single event")
		createModule("xxx", TEST_MODULE_CODE, "Timetabling Module")
		val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME)
		addStudentToGroup(P.Student1.usercode,setId, "Group 1")
		createSmallGroupEvent(setId,"Test timetabling", weekRange = "47")
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
