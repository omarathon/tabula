package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindException
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.FileAttachment

class CreateMeetingRecordCommandTest extends AppContextTestBase with Mockito {

	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)
	val marchHare = dateTime(2013, DateTimeConstants.MARCH).toLocalDate

	@Transactional
	@Test
	def validMeeting = withUser("cuscav") { withFakeTime(aprilFool) {

		val ps = mock[ProfileService]
		val creator = new StaffMember("9876543")
		val student = mock[StudentMember]
		val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "0123456/1")
		relationship.profileService = ps
		ps.getStudentBySprCode("0123456/1") returns (Some(student))

		val cmd = new CreateMeetingRecordCommand(creator, relationship)
		cmd.title = "A title"
		cmd.meetingDate  = dateTime(3903, DateTimeConstants.MARCH).toLocalDate // it's the future

		// check invalid future date
		var errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("meetingDate")
		errors.getFieldError.getCode should be ("meetingRecord.date.future")

		cmd.meetingDate = dateTime(2007, DateTimeConstants.MARCH).toLocalDate // > 5 years ago

		// check invalid past date
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("meetingDate")
		errors.getFieldError.getCode should be ("meetingRecord.date.prehistoric")

		cmd.meetingDate = marchHare
		cmd.title = ""

		// check invalid empty title
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("title")
		errors.getFieldError.getCode should be ("NotEmpty")

		cmd.title = "A good title"

		// check valid
		errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (false)

		// add some text and apply
		cmd.description = "Lovely words"

		// try adding a file
		var uploadedFile =  new UploadedFile
		var mpFile = mock[MultipartFile]
		uploadedFile.upload.add(mpFile)

		var fileAttach = new FileAttachment
		fileAttach.name = "Beltane"
		uploadedFile.attached.add(fileAttach)

		cmd.file = uploadedFile

		val meeting = cmd.apply()

		meeting.creator should be (creator)
		meeting.creationDate should be (aprilFool)
		meeting.lastUpdatedDate should be (aprilFool)
		meeting.title should be ("A good title")
		meeting.description should be ("<p>Lovely words</p>")
		meeting.meetingDate.toLocalDate should be (marchHare)
		meeting.attachments.get(0).name should be ("Beltane")
	}}
}
