package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.transaction.annotation.Transactional
import org.joda.time.LocalTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime

class SmallGroupPersistenceTest extends AppContextTestBase {
	
	@Autowired var service: ModuleAndDepartmentService = _
	
	@Transactional
	@Test def itWorks {
		// Use data from data.sql
		{
			val cs108 = service.getModuleByCode("cs108").get
			
			val group1 = new SmallGroup(cs108)
			group1.name = "Group 1"
			group1.format = SmallGroupFormat.Seminar
			
			cs108.groups.add(group1)
			session.saveOrUpdate(cs108)
			
			group1.students.addUser("cuscav")
			session.saveOrUpdate(group1)
			
			val event1 = new SmallGroupEvent(group1)
			event1.title = "Event 1"
			event1.location = "A0.01"
			event1.weekRanges = Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)) // Weeks 1, 2, 3, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24
			event1.day = DayOfWeek.Tuesday
			event1.startTime = new LocalTime(15, 0) // 3pm
			event1.endTime = event1.startTime.plusHours(1) // 4pm
			
			val event2 = new SmallGroupEvent(group1)
			event2.title = "Event 2"
			event2.location = "A0.01"
			event2.weekRanges = Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)) // Weeks 1, 2, 3, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24
			event2.day = DayOfWeek.Thursday
			event2.startTime = new LocalTime(13, 0) // 1pm
			event2.endTime = event2.startTime.plusHours(1) // 2pm
			
			group1.events.add(event1)
			group1.events.add(event2)
			
			session.saveOrUpdate(group1)
			
			event1.tutors.addUser("cusebr")
			event2.tutors.addUser("cusfal")
			
			session.saveOrUpdate(event1)
			session.saveOrUpdate(event2)
			
			session.flush
			session.clear
		}
		
		val cs108 = service.getModuleByCode("cs108").get
		val group1 = cs108.groups.get(0)
		val event1 = group1.events.asScala.find(_.title == "Event 1").head
		val event2 = group1.events.asScala.find(_.title == "Event 2").head
		
		// Check a few choice fields to check they're persisted right
		group1.format should be (SmallGroupFormat.Seminar)
		group1.academicYear should be (AcademicYear.guessByDate(DateTime.now))
		
		event1.day should be (DayOfWeek.Tuesday)
		event1.startTime should be (new LocalTime(15, 0))
		event1.endTime should be (new LocalTime(16, 0))
		event1.weekRanges should be (Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)))
		
		event2.day should be (DayOfWeek.Thursday)
		event2.startTime should be (new LocalTime(13, 0))
		event2.endTime should be (new LocalTime(14, 0))
		event2.weekRanges should be (Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)))
	}

}