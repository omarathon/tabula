package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.{AcademicYear, TestBase}
import scala.xml.XML
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.LocalTime

class TimetableFetchingServiceTest extends TestBase {
	
	@Test def parseXML() {
		val events = ScientiaHttpTimetableFetchingService.parseXml(XML.loadString(TimetableEvents), AcademicYear(2012))
		events.size should be (10)
		events(0) should be (TimetableEvent(
			name="CS132L",
			description="",
			startTime=new LocalTime(12, 0),
			endTime=new LocalTime(13, 0),
			weekRanges=Seq(WeekRange(6, 10)),
			day=DayOfWeek.Friday,
			eventType=TimetableEventType.Lecture,
			location=Some("L5"),
			moduleCode="CS132",
			staffUniversityIds=Seq("1170047"),
		  year = AcademicYear(2012)
		))
	}
	
	val TimetableEvents = """<?xml version="1.0" encoding="UTF-8"?>
<Data>
   <Activities>
      <Activity>
         <name>CS132L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>6-10</weeks>
         <day>4</day>
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
         <name>CS133Ltest</name>
         <description />
         <start>09:00</start>
         <end>11:00</end>
         <weeks>31</weeks>
         <day>2</day>
         <type>LEC</type>
         <rooms>
            <room>MS.02</room>
         </rooms>
         <modules>
            <module>CS133</module>
         </modules>
         <staffmembers>
            <staffmember>1170047</staffmember>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS130L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>1-10</weeks>
         <day>3</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS130</module>
         </modules>
         <staffmembers>
            <staffmember>1170588</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS133L</name>
         <description />
         <start>14:00</start>
         <end>15:00</end>
         <weeks>1-10, 15-18, 20-24</weeks>
         <day>1</day>
         <type>LEC</type>
         <rooms>
            <room>H0.52</room>
         </rooms>
         <modules>
            <module>CS133</module>
         </modules>
         <staffmembers>
            <staffmember>1170047</staffmember>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS130L</name>
         <description />
         <start>14:00</start>
         <end>15:00</end>
         <weeks>1-10</weeks>
         <day>4</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS130</module>
         </modules>
         <staffmembers>
            <staffmember>1170588</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS126L</name>
         <description />
         <start>15:00</start>
         <end>16:00</end>
         <weeks>15-24</weeks>
         <day>1</day>
         <type>LEC</type>
         <rooms>
            <room>PLT</room>
         </rooms>
         <modules>
            <module>CS126</module>
         </modules>
         <staffmembers>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS126L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>15-24</weeks>
         <day>2</day>
         <type>LEC</type>
         <rooms>
            <room>H0.52</room>
         </rooms>
         <modules>
            <module>CS126</module>
         </modules>
         <staffmembers>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS126L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>15-24</weeks>
         <day>3</day>
         <type>LEC</type>
         <rooms>
            <room>PLT</room>
         </rooms>
         <modules>
            <module>CS126</module>
         </modules>
         <staffmembers>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS131L</name>
         <description />
         <start>17:00</start>
         <end>18:00</end>
         <weeks>15-24</weeks>
         <day>3</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS131</module>
         </modules>
         <staffmembers>
            <staffmember>1174683</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS131L</name>
         <description />
         <start>10:00</start>
         <end>11:00</end>
         <weeks>15-24</weeks>
         <day>2</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS131</module>
         </modules>
         <staffmembers>
            <staffmember>1174683</staffmember>
         </staffmembers>
      </Activity>
   </Activities>
</Data>"""

}