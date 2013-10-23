package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{MeetingRecordApproval, MeetingRecord, StudentRelationship, Member}
import org.hibernate.criterion.{Restrictions,Order}
import scala.collection.JavaConversions._
import uk.ac.warwick.spring.Wire

trait MeetingRecordDao {
	def saveOrUpdate(meeting: MeetingRecord)
	def saveOrUpdate(approval: MeetingRecordApproval)
	def list(rel: Set[StudentRelationship], currentUser: Member): Seq[MeetingRecord]
	def list(rel: StudentRelationship): Seq[MeetingRecord]
	def get(id: String): Option[MeetingRecord]
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms {

	def saveOrUpdate(meeting: MeetingRecord) = session.saveOrUpdate(meeting)

	def saveOrUpdate(approval: MeetingRecordApproval) = session.saveOrUpdate(approval)

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

	def list(rel: StudentRelationship): Seq[MeetingRecord] = {
		session.newCriteria[MeetingRecord]
			.add(Restrictions.eq("relationship", rel))
			.add(is("deleted", false))
			.addOrder(Order.desc("meetingDate"))
			.addOrder(Order.desc("lastUpdatedDate"))
			.seq
	}


	def get(id: String) = getById[MeetingRecord](id)
}

trait MeetingRecordDaoComponent {
	val meetingRecordDao: MeetingRecordDao
}
trait AutowiringMeetingRecordDaoComponent extends MeetingRecordDaoComponent{
	val meetingRecordDao = Wire.auto[MeetingRecordDao]
}
