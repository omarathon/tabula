package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationship}
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, AutowiringRelationshipServiceComponent, MeetingRecordServiceComponent, RelationshipServiceComponent}

object MigrateMeetingRecordsFromOldRelationshipsCommand {
	def apply(student: StudentMember) =
		new MigrateMeetingRecordsFromOldRelationshipsCommandInternal(student)
			with AutowiringRelationshipServiceComponent
			with AutowiringMeetingRecordServiceComponent
			with ComposableCommand[Unit]
			with MigrateMeetingRecordsFromOldRelationshipsValidation
			with ExpireRelationshipsOnOldCoursesPermissions
			with MigrateMeetingRecordsFromOldRelationshipsCommandState
			with Unaudited
}


class MigrateMeetingRecordsFromOldRelationshipsCommandInternal(val student: StudentMember) extends CommandInternal[Unit] {

	self: MigrateMeetingRecordsFromOldRelationshipsCommandState with MeetingRecordServiceComponent =>

	override def applyInternal() = {
		migrations.foreach{ case(from, to) => meetingRecordService.migrate(from, to) }
	}

}

trait MigrateMeetingRecordsFromOldRelationshipsValidation extends SelfValidating {

	self: MigrateMeetingRecordsFromOldRelationshipsCommandState =>

	override def validate(errors: Errors) {
		if (migrations.isEmpty) {
			errors.reject("No meetings to migrate")
		}
	}

}

trait MigrateMeetingRecordsFromOldRelationshipsCommandState extends ExpireRelationshipsOnOldCoursesCommandState {

	self: MeetingRecordServiceComponent with RelationshipServiceComponent =>

	lazy val migrations: Map[StudentRelationship, StudentRelationship] = {
		studentRelationships.groupBy(_.agent).flatMap{case(agent, agentRelationships) =>
			agentRelationships.filter(!_.isCurrent).flatMap(nonCurrentRelationship => {
				val meetingRecords = meetingRecordService.listAll(nonCurrentRelationship)
				val correspondingRelationship = agentRelationships.find(r =>
					r.isCurrent && sprMatch(r, nonCurrentRelationship) && departmentsMatch(r, nonCurrentRelationship)
				)
				if (meetingRecords.isEmpty || correspondingRelationship.isEmpty)
					None
				else {
					Option(nonCurrentRelationship -> correspondingRelationship.get)
				}
			})
		}
	}

	private def sprMatch(r1: StudentRelationship, r2: StudentRelationship) =
		r1.studentCourseDetails.sprCode == r2.studentCourseDetails.sprCode

	private def departmentsMatch(r1: StudentRelationship, r2: StudentRelationship) =
		r1.studentCourseDetails.latestStudentCourseYearDetails.enrolmentDepartment == r2.studentCourseDetails.latestStudentCourseYearDetails.enrolmentDepartment
}
