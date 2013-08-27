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
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.springframework.validation.BindException
import org.springframework.transaction.annotation.Transactional
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.data.model.MeetingFormat._
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.StudentRelationshipType


class DownloadMeetingRecordCommandTest extends AppContextTestBase with Mockito {

	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)

	@Transactional
	@Test
	def validMeeting = withUser("cusdx") { withFakeTime(aprilFool) {

		val ps = mock[ProfileService]

		val creator = transactional { tx =>
			val m = new StaffMember("9876543")
			m.userId = "staffmember"
			session.save(m)
			m
		}

		val student = transactional { tx =>
			val m = new StudentMember("1170836")
			m.userId = "studentmember"
			session.save(m)
			m
		}

		val relationship = transactional { tx =>
			val relationship = StudentRelationship("Professor A Tutor", new StudentRelationshipType, "0123456/1")
			relationship.profileService = ps
			ps.getStudentBySprCode("0123456/1") returns (Some(student))

			session.save(relationship)
			relationship
		}

		val createMeetingRecordCommand = new CreateMeetingRecordCommand(creator, relationship, false)
		createMeetingRecordCommand.title = "Title"
		createMeetingRecordCommand.format = FaceToFace
		createMeetingRecordCommand.meetingDate  = dateTime(3903, DateTimeConstants.MARCH).toLocalDate // it's the future
		createMeetingRecordCommand.description = "Lovely words"

		// add a file
		val uploadedFile =  new UploadedFile
		val mpFile = mock[MultipartFile]
		uploadedFile.upload.add(mpFile)

		val fileAttach = new FileAttachment
		fileAttach.name = "Beltane"
		uploadedFile.attached.add(fileAttach)

		createMeetingRecordCommand.file = uploadedFile

		val meeting = transactional { tx => createMeetingRecordCommand.applyInternal() }

		// test to see if DownloadMeetingRecordFilesCommand.apply() can be used to get the file
		val downloadCommand = new DownloadMeetingRecordFilesCommand(meeting)

		// normally for single files the filename is set in the command as it is a path variable (I think!)
		downloadCommand.filename = "Beltane"
		val retSingle = downloadCommand.applyInternal()
		val rendFile = retSingle.get
		rendFile.filename should be ("Beltane")
	}}
}
