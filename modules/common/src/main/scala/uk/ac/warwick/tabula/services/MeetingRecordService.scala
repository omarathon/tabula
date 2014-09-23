package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
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
	def list(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[MeetingRecord]
	def listScheduled(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[ScheduledMeetingRecord]
	def listAll(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[AbstractMeetingRecord]
	def list(rel: StudentRelationship): Seq[MeetingRecord]
	def get(id: String): Option[AbstractMeetingRecord]
	def purge(meeting: AbstractMeetingRecord): Unit
	def getAcademicYear(meeting: AbstractMeetingRecord): Option[AcademicYear]
	def getAcademicYear(id: String): Option[AcademicYear]
}


abstract class AbstractMeetingRecordService extends MeetingRecordService {
	self: MeetingRecordDaoComponent with TermServiceComponent =>

	def saveOrUpdate(meeting: MeetingRecord) = meetingRecordDao.saveOrUpdate(meeting)
	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord) = meetingRecordDao.saveOrUpdate(scheduledMeeting)
	def saveOrUpdate(meeting: AbstractMeetingRecord) = meetingRecordDao.saveOrUpdate(meeting)
	def saveOrUpdate(approval: MeetingRecordApproval) = meetingRecordDao.saveOrUpdate(approval)
	def list(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[MeetingRecord] = meetingRecordDao.list(rel, currentMember)
	def listScheduled(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[ScheduledMeetingRecord] =
		meetingRecordDao.listScheduled(rel, currentMember)
	def list(rel: StudentRelationship): Seq[MeetingRecord] = meetingRecordDao.list(rel)
	def listAll(rel: Set[StudentRelationship], currentMember: Option[Member]): Seq[AbstractMeetingRecord] = {
		(meetingRecordDao.list(rel, currentMember) ++ meetingRecordDao.listScheduled(rel, currentMember)).sorted
	}
	def get(id: String): Option[AbstractMeetingRecord] =  Option(id) match {
		case Some(uid) if uid.nonEmpty => meetingRecordDao.get(uid)
		case _ => None
	}
	def purge(meeting: AbstractMeetingRecord): Unit = meetingRecordDao.purge(meeting)
	def getAcademicYear(meeting: AbstractMeetingRecord): Option[AcademicYear] = Some(AcademicYear.findAcademicYearContainingDate(meeting.meetingDate, termService))
  def getAcademicYear(id: String): Option[AcademicYear] = Option(id).flatMap(get(_)).flatMap(getAcademicYear(_))
}

@Service("meetingRecordService")
class MeetingRecordServiceImpl
	extends AbstractMeetingRecordService
	with AutowiringMeetingRecordDaoComponent
  with AutowiringTermServiceComponent