package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import org.hibernate.`type`._
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.StudentRelationship
import org.hibernate.criterion.{Restrictions,Order}
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire

trait MeetingRecordDao {
	def saveOrUpdate(meeting: MeetingRecord)
	def list(rel: Set[StudentRelationship], currentUser: Member): Seq[MeetingRecord]
	def get(id: String): Option[MeetingRecord]
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms {

	def saveOrUpdate(meeting: MeetingRecord) = session.saveOrUpdate(meeting)

	def list(rel: Set[StudentRelationship], currentUser: Member): Seq[MeetingRecord] = {
		if (rel.isEmpty)
			Seq()
		else
			session.newCriteria[MeetingRecord]
					.add(Restrictions.in("relationship", rel))
					// and only pick records where deleted = 0 or the current user id is the creator id
					// - so that no-one can see records created and deleted by someone else
					.add(Restrictions.disjunction()
							.add(is("deleted", false))
							.add(is("creator", currentUser))
					)
					.addOrder(Order.desc("meetingDate"))
					.addOrder(Order.desc("lastUpdatedDate"))
					.seq


	}


	def get(id: String) = getById[MeetingRecord](id);
}

trait MeetingRecordDaoComponent{
	var meetingRecordDao:MeetingRecordDao
}
trait AutowiringMeetingRecordDaoComponent extends MeetingRecordDaoComponent{
	var meetingRecordDao = Wire.auto[MeetingRecordDao]
}
