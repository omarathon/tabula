package uk.ac.warwick.tabula.groups.commands.admin
import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

import org.joda.time.LocalTime
import scala.collection.JavaConverters._

class CreateSmallGroupSetCommandTest extends AppContextTestBase {
	
	@Transactional
	@Test def itWorks {
		val f = MyFixtures()
		
		val cmd = new CreateSmallGroupSetCommand(f.module1)
		cmd.name = "Terry"
		cmd.format = SmallGroupFormat.Seminar
		
		// Create two groups with two events
		val group1Cmd = cmd.groups.get(0) 
		group1Cmd.name = "Steve"
		
		val g1Event1Cmd = group1Cmd.events.get(0)
		g1Event1Cmd.weekRanges = Seq(WeekRange(1, 5), WeekRange(7, 10))
		g1Event1Cmd.day = DayOfWeek.Tuesday
		g1Event1Cmd.startTime = new LocalTime(13, 0)
		g1Event1Cmd.endTime = new LocalTime(14, 0)
		g1Event1Cmd.location = "John's house"
		g1Event1Cmd.title = "John"
		
		val g1Event2Cmd = group1Cmd.events.get(1)
		g1Event2Cmd.weekRanges = Seq(WeekRange(2, 10))
		g1Event2Cmd.day = DayOfWeek.Wednesday
		g1Event2Cmd.startTime = new LocalTime(15, 0)
		g1Event2Cmd.endTime = new LocalTime(16, 0)
		g1Event2Cmd.location = "Quentin's basement"
		g1Event2Cmd.title = "Quentin"
			
		val group2Cmd = cmd.groups.get(1) 
		group2Cmd.name = "Stuart"
		
		val g2Event1Cmd = group2Cmd.events.get(0)
		g2Event1Cmd.weekRanges = Seq(WeekRange(1, 10), WeekRange(15, 24))
		g2Event1Cmd.day = DayOfWeek.Monday
		g2Event1Cmd.startTime = new LocalTime(9, 0)
		g2Event1Cmd.endTime = new LocalTime(12, 0)
		g2Event1Cmd.location = "Simon's shack"
		g2Event1Cmd.title = "Simon"
		
		val g2Event2Cmd = group2Cmd.events.get(1)
		g2Event2Cmd.weekRanges = Seq(WeekRange(2))
		g2Event2Cmd.day = DayOfWeek.Friday
		g2Event2Cmd.startTime = new LocalTime(9, 30)
		g2Event2Cmd.endTime = new LocalTime(10, 30)
		g2Event2Cmd.location = "Joe's apartment"
		g2Event2Cmd.title = "Joe"
			
		val set = cmd.apply()
		set.name should be ("Terry")
		set.format should be (SmallGroupFormat.Seminar)
		set.groups.size should be (2)
		
		val group1 = set.groups.asScala.find(_.name == "Steve").head
		group1.events.size should be (2)
		
		val g1e1 = group1.events.asScala.find(_.title == "John").head
		g1e1.weekRanges should be (Seq(WeekRange(1, 5), WeekRange(7, 10)))
		g1e1.day should be (DayOfWeek.Tuesday)
		g1e1.startTime should be (new LocalTime(13, 0))
		g1e1.endTime should be (new LocalTime(14, 0))
		g1e1.location should be ("John's house")
				
		val g1e2 = group1.events.asScala.find(_.title == "Quentin").head
		g1e2.weekRanges should be (Seq(WeekRange(2, 10)))
		g1e2.day should be (DayOfWeek.Wednesday)
		g1e2.startTime should be (new LocalTime(15, 0))
		g1e2.endTime should be (new LocalTime(16, 0))
		g1e2.location should be ("Quentin's basement")
		
		val group2 = set.groups.asScala.find(_.name == "Stuart").head
		group2.events.size should be (2)
		
		val g2e1 = group2.events.asScala.find(_.title == "Simon").head
		g2e1.weekRanges should be (Seq(WeekRange(1, 10), WeekRange(15, 24)))
		g2e1.day should be (DayOfWeek.Monday)
		g2e1.startTime should be (new LocalTime(9, 0))
		g2e1.endTime should be (new LocalTime(12, 0))
		g2e1.location should be ("Simon's shack")
			
		val g2e2 = group2.events.asScala.find(_.title == "Joe").head
		g2e2.weekRanges should be (Seq(WeekRange(2)))
		g2e2.day should be (DayOfWeek.Friday)
		g2e2.startTime should be (new LocalTime(9, 30))
		g2e2.endTime should be (new LocalTime(10, 30))
		g2e2.location should be ("Joe's apartment")
	}
	
	case class MyFixtures() {
		val department = Fixtures.department(code="ls", name="Life Sciences")
        val module1 = Fixtures.module(code="ls101")
        val module3 = Fixtures.module(code="ls103")
        
        session.save(department)
        session.save(module1)
        session.save(module3)
	}

}