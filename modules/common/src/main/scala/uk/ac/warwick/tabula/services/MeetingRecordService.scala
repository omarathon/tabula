package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{MeetingRecordDaoComponent, AutowiringMeetingRecordDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, StudentRelationship, MeetingRecordApproval, ScheduledMeetingRecord, MeetingRecord, Member}

trait MeetingRecordServiceComponent {
	def meetingRecordService: MeetingRecordService
}

trait AutowiringMeetingRecordServiceComponent extends MeetingRecordServiceComponent {
	var meetingRecordService = Wire[MeetingRecordService]
}

trait MeetingRecordService {

	def saveOrUpdate(meeting: MeetingRecord)
	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord)
	def saveOrUpdate(meeting: AbstractMeetingRecord)
	def saveOrUpdate(approval: MeetingRecordApproval)
	def list(rel: Set[StudentRelationship[_]], currentMember: Member): Seq[MeetingRecord]
	def listScheduled(rel: Set[StudentRelationship[_]], currentMember: Member): Seq[ScheduledMeetingRecord]
	def listAll(rel: Set[StudentRelationship[_]], currentMember: Member): Seq[AbstractMeetingRecord]
	def list(rel: StudentRelationship[_]): Seq[MeetingRecord]
	def get(id: String): Option[AbstractMeetingRecord]
	def purge(meeting: AbstractMeetingRecord): Unit

}


abstract class AbstractMeetingRecordService extends MeetingRecordService {
	self: MeetingRecordDaoComponent =>

	def saveOrUpdate(meeting: MeetingRecord) = meetingRecordDao.saveOrUpdate(meeting)
	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord) = meetingRecordDao.saveOrUpdate(scheduledMeeting)
	def saveOrUpdate(meeting: AbstractMeetingRecord) = meetingRecordDao.saveOrUpdate(meeting)
	def saveOrUpdate(approval: MeetingRecordApproval) = meetingRecordDao.saveOrUpdate(approval)
	def list(rel: Set[StudentRelationship[_]], currentMember: Member): Seq[MeetingRecord] = meetingRecordDao.list(rel, currentMember)
	def listScheduled(rel: Set[StudentRelationship[_]], currentMember: Member): Seq[ScheduledMeetingRecord] = meetingRecordDao.listScheduled(rel, currentMember)
	def list(rel: StudentRelationship[_]): Seq[MeetingRecord] = meetingRecordDao.list(rel)
	def listAll(rel: Set[StudentRelationship[_]], currentMember: Member): Seq[AbstractMeetingRecord] = {
		(meetingRecordDao.list(rel, currentMember) ++ meetingRecordDao.listScheduled(rel, currentMember)).sorted
	}
	def get(id: String): Option[AbstractMeetingRecord] = meetingRecordDao.get(id)
	def purge(meeting: AbstractMeetingRecord): Unit = meetingRecordDao.purge(meeting)

}

@Service("meetingRecordService")
class MeetingRecordServiceImpl
	extends AbstractMeetingRecordService
	with AutowiringMeetingRecordDaoComponent