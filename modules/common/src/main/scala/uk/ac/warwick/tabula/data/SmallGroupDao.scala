package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventOccurrence, SmallGroupEvent, SmallGroup, SmallGroupSet, SmallGroupEventAttendance}
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import scala.collection.JavaConverters._

trait SmallGroupDaoComponent {
	val smallGroupDao: SmallGroupDao
}

trait AutowiringSmallGroupDaoComponent extends SmallGroupDaoComponent {
	val smallGroupDao = Wire[SmallGroupDao]
}

trait SmallGroupDao {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def saveOrUpdate(occurrence: SmallGroupEventOccurrence)
	def saveOrUpdate(attendance: SmallGroupEventAttendance)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote)
	
	def findByModuleAndYear(module: Module, year: AcademicYear): Seq[SmallGroup]

	def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int): Option[SmallGroupEventOccurrence]
	def findSmallGroupOccurrencesByGroup(group: SmallGroup): Seq[SmallGroupEventOccurrence]
	
	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendance]
	def deleteAttendance(attendance: SmallGroupEventAttendance): Unit
	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote]
	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote]
}

@Repository
class SmallGroupDaoImpl extends SmallGroupDao with Daoisms {
	import Order._
	
	def getSmallGroupSetById(id: String) = getById[SmallGroupSet](id)
	def getSmallGroupById(id: String) = getById[SmallGroup](id)
	def getSmallGroupEventById(id: String) = getById[SmallGroupEvent](id)
	def getSmallGroupEventOccurrenceById(id: String) = getById[SmallGroupEventOccurrence](id)
	def saveOrUpdate(smallGroupSet: SmallGroupSet) = session.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = session.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = session.saveOrUpdate(smallGroupEvent)
	def saveOrUpdate(occurrence: SmallGroupEventOccurrence) = session.saveOrUpdate(occurrence)
	def saveOrUpdate(attendance: SmallGroupEventAttendance) = session.saveOrUpdate(attendance)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote) = session.saveOrUpdate(note)

	def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int) =
		session.newCriteria[SmallGroupEventOccurrence]
			.add(is("event", event))
			.add(is("week", week))
			.uniqueResult

	def findByModuleAndYear(module: Module, year: AcademicYear) =
		session.newCriteria[SmallGroup]
			.createAlias("groupSet", "set")
			.add(is("set.module", module))
			.add(is("set.academicYear", year))
			.seq
			
	def findSmallGroupOccurrencesByGroup(group: SmallGroup) = 
		session.newCriteria[SmallGroupEventOccurrence]
			.createAlias("event", "event")
			.add(is("event.group", group))
			.addOrder(asc("week"))
			.addOrder(asc("event.day"))
			.seq
			
	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendance] =
		session.newCriteria[SmallGroupEventAttendance]
				.add(is("universityId", studentId))
				.add(is("occurrence", occurrence))
				.uniqueResult
				
	def deleteAttendance(attendance: SmallGroupEventAttendance): Unit = session.delete(attendance)

	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote] = {
		session.newCriteria[SmallGroupEventAttendanceNote]
			.add(is("student.id", studentId))
			.add(is("occurrence", occurrence))
			.uniqueResult
	}

	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote] = {
		if(studentIds.size == 0 || occurrences.size == 0)
			return Seq()

		session.newCriteria[SmallGroupEventAttendanceNote]
			.add(in("student.id", studentIds.asJava))
			.add(in("occurrence", occurrences.asJava))
			.seq
	}
}
