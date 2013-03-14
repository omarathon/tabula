package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import org.hibernate.`type`._
import uk.ac.warwick.tabula.data.model.MeetingRecord

trait MeetingRecordDao {
	def saveOrUpdate(meeting: MeetingRecord)
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms {

	def saveOrUpdate(meeting: MeetingRecord) = session.saveOrUpdate(meeting)
}