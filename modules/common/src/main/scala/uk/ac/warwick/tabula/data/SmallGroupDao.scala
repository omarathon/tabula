package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventOccurrence, SmallGroupEvent, SmallGroup, SmallGroupSet}
import org.hibernate.criterion.Restrictions
import org.springframework.stereotype.Repository

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

	def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int): Option[SmallGroupEventOccurrence]
}

@Repository
class SmallGroupDaoImpl extends SmallGroupDao with Daoisms {
	def getSmallGroupSetById(id: String) = getById[SmallGroupSet](id)
	def getSmallGroupById(id: String) = getById[SmallGroup](id)
	def getSmallGroupEventById(id: String) = getById[SmallGroupEvent](id)
	def getSmallGroupEventOccurrenceById(id: String) = getById[SmallGroupEventOccurrence](id)
	def saveOrUpdate(smallGroupSet: SmallGroupSet) = session.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = session.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = session.saveOrUpdate(smallGroupEvent)
	def saveOrUpdate(occurrence: SmallGroupEventOccurrence) = session.saveOrUpdate(occurrence)

	def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int) =
		session.newCriteria[SmallGroupEventOccurrence]
			.add(is("event", event))
			.add(is("week", week))
			.uniqueResult
}
