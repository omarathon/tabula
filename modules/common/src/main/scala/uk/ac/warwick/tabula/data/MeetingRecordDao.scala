package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import org.hibernate.`type`._
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.StudentRelationship
import org.hibernate.criterion.{Restrictions,Order}
import scala.collection.JavaConversions._

trait MeetingRecordDao {
	def saveOrUpdate(meeting: MeetingRecord)
	def list(rel: Set[StudentRelationship]): Seq[MeetingRecord]
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms {

	def saveOrUpdate(meeting: MeetingRecord) = session.saveOrUpdate(meeting)
	
	def list(rel: Set[StudentRelationship]): Seq[MeetingRecord] = {
		if (rel.isEmpty)
			Seq()
		else
			session.newCriteria[MeetingRecord]
					.add(Restrictions.in("relationship", rel))
					.addOrder(Order.desc("meetingDate"))
					.seq
	}
}