package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Order, Projections, Restrictions}
import org.hibernate.sql.JoinType
import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

import scala.collection.JavaConverters._

trait MeetingRecordDao {
	def saveOrUpdate(meeting: MeetingRecord)
	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord)
	def saveOrUpdate(meeting: AbstractMeetingRecord)
	def saveOrUpdate(approval: MeetingRecordApproval)
	def listScheduled(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[ScheduledMeetingRecord]
	def list(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[MeetingRecord]
	def list(rel: StudentRelationship): Seq[MeetingRecord]
	def listScheduled(rel: StudentRelationship): Seq[ScheduledMeetingRecord]
	def countPendingApprovals(universityId: String): Int
	def get(id: String): Option[AbstractMeetingRecord]
	def purge(meeting: AbstractMeetingRecord): Unit
	def purge(approval: MeetingRecordApproval): Unit
	def migrate(from: StudentRelationship, to: StudentRelationship): Unit
	def unconfirmedScheduledCount(relationships: Seq[StudentRelationship]): Map[StudentRelationship, Int]
	def listAllOnOrAfter(localDate: LocalDate): Seq[MeetingRecord]
}

@Repository
class MeetingRecordDaoImpl extends MeetingRecordDao with Daoisms with TaskBenchmarking {

	def saveOrUpdate(meeting: MeetingRecord): Unit = session.saveOrUpdate(meeting)

	def saveOrUpdate(scheduledMeeting: ScheduledMeetingRecord): Unit = session.saveOrUpdate(scheduledMeeting)

	def saveOrUpdate(meeting: AbstractMeetingRecord): Unit = session.saveOrUpdate(meeting)

	def saveOrUpdate(approval: MeetingRecordApproval): Unit = session.saveOrUpdate(approval)

	def list(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[MeetingRecord] = {
		if (rel.isEmpty)
			Seq()
		else
			addMeetingRecordListRestrictionsAndList(() => session.newCriteria[MeetingRecord], rel, currentUser).distinct
	}

	def listScheduled(rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[ScheduledMeetingRecord] = {
		if (rel.isEmpty)
			Seq()
		else
			addMeetingRecordListRestrictionsAndList(() => session.newCriteria[ScheduledMeetingRecord], rel, currentUser).distinct
	}

	private def addMeetingRecordListRestrictionsAndList[A <: AbstractMeetingRecord](criteriaFactory: () => ScalaCriteria[A], rel: Set[StudentRelationship], currentUser: Option[Member]): Seq[A] = {
		// only pick records where deleted = 0 or the current user id is the creator id
		// - so that no-one can see records created and deleted by someone else
		val c = () => {
			val criteria = criteriaFactory.apply()

			currentUser match {
				case None | Some(_: RuntimeMember) => criteria.add(is("deleted", false))
				case Some(cu) => criteria.add(Restrictions.disjunction()
					.add(is("deleted", false))
					.add(is("creator", cu))
				)
			}

			criteria.addOrder(Order.desc("meetingDate")).addOrder(Order.desc("lastUpdatedDate"))

			criteria.createAlias("_relationships", "relationships", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.or(
					safeIn("relationship", rel.toSeq),
					safeIn("relationships.id", rel.toSeq.map(_.id))
				))
		}

		val meetings = c.apply().seq
		meetings.sortBy(m => (m.meetingDate, m.lastUpdatedDate))(Ordering[(DateTime,DateTime)].reverse)
	}

	def list(rel: StudentRelationship): Seq[MeetingRecord] = {
		session.newCriteria[MeetingRecord]
			.createAlias("_relationships", "relationships")
  		.add(Restrictions.or(
				Restrictions.eq("relationship", rel),
				Restrictions.eq("relationships.id", rel.id)
			))
			.add(is("deleted", false))
			.addOrder(Order.desc("meetingDate"))
			.addOrder(Order.desc("lastUpdatedDate"))
			.seq
	}

	def listScheduled(rel: StudentRelationship): Seq[ScheduledMeetingRecord] = {
		session.newCriteria[ScheduledMeetingRecord]
			.createAlias("_relationships", "relationships")
			.add(Restrictions.or(
				Restrictions.eq("relationship", rel),
				Restrictions.eq("relationships.id", rel.id)
			))
			.add(is("deleted", false))
			.addOrder(Order.desc("meetingDate"))
			.addOrder(Order.desc("lastUpdatedDate"))
			.seq
	}

	def listAllOnOrAfter(localDate: LocalDate): Seq[MeetingRecord] = {
		session.newCriteria[MeetingRecord]
  		.add(Restrictions.ge("meetingDate", localDate.toDateTimeAtStartOfDay))
  		.seq
	}

	def countPendingApprovals(universityId: String): Int = {
		session.newCriteria[MeetingRecordApproval]
			.createAlias("meetingRecord", "meetingRecord")
			.add(is("approver.universityId", universityId))
			.add(is("state", MeetingApprovalState.Pending))
			.add(is("meetingRecord.deleted", false))
			.count.intValue()
	}

	def get(id: String): Option[AbstractMeetingRecord] = getById[AbstractMeetingRecord](id)

	def purge(meeting: AbstractMeetingRecord): Unit = {
		session.delete(meeting)
		session.flush()
	}

	def purge(approval: MeetingRecordApproval): Unit = {
		session.delete(approval)
		session.flush()
	}

	def migrate(from: StudentRelationship, to: StudentRelationship): Unit = benchmarkTask("migrate") {
		(list(from) ++ listScheduled(from)).foreach { meetingRecord =>
			meetingRecord.replaceParticipant(original = from, replacement = to)
			saveOrUpdate(meetingRecord)
		}
	}

	def unconfirmedScheduledCount(relationships: Seq[StudentRelationship]): Map[StudentRelationship, Int] = benchmarkTask("unconfirmedScheduledCount") {
		val hasOneRelationship = safeInSeqWithProjection[ScheduledMeetingRecord, Array[java.lang.Object]](
			() => {
				session.newCriteria[ScheduledMeetingRecord]
  				.add(Restrictions.isNotNull("relationship"))
					.add(Restrictions.lt("meetingDate", DateTime.now))
			},
			Projections.projectionList()
				.add(Projections.groupProperty("relationship"))
				.add(Projections.count("relationship")),
			"relationship",
			relationships
		).map { objArray =>
			objArray(0).asInstanceOf[StudentRelationship] -> objArray(1).asInstanceOf[Long].toInt
		}.toMap

		val hasManyRelationships = safeInSeqWithProjection[StudentRelationship, Array[java.lang.Object]](
			() => {
				session.newCriteria[StudentRelationship]
					.createAlias("meetingRecords", "meetingRecords")
  				.add(Restrictions.eq("meetingRecords.class", classOf[ScheduledMeetingRecord]))
					.add(Restrictions.isNull("meetingRecords.relationship"))
					.add(Restrictions.lt("meetingRecords.meetingDate", DateTime.now))
			},
			Projections.projectionList()
				.add(Projections.groupProperty("id"))
				.add(Projections.count("id")),
			"id",
			relationships.map(_.id),
		).map { objArray =>
			relationships.find(_.id == objArray(0).asInstanceOf[String]).get -> objArray(1).asInstanceOf[Long].toInt
		}.toMap

		hasOneRelationship ++ hasManyRelationships.map { case (rel, count) => rel -> (count + hasOneRelationship.getOrElse(rel, 0)) }
	}
}

trait MeetingRecordDaoComponent {
	val meetingRecordDao: MeetingRecordDao
}
trait AutowiringMeetingRecordDaoComponent extends MeetingRecordDaoComponent{
	val meetingRecordDao: MeetingRecordDao = Wire.auto[MeetingRecordDao]
}
