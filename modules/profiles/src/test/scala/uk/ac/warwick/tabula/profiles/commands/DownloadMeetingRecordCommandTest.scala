package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTimeConstants
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.Mockito
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.MeetingFormat._
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.ExternalStudentRelationship
import uk.ac.warwick.tabula.Fixtures


class DownloadMeetingRecordCommandTest extends AppContextTestBase with Mockito {

	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)

	@Transactional
	@Test
	def validMeeting() = withUser("cusdx") { withFakeTime(aprilFool) {

		val creator = transactional { tx =>
			val m = new StaffMember("9876543")
			m.userId = "staffmember"
			session.save(m)
			m
		}

		val student = transactional { tx =>
			val m = Fixtures.student(universityId="1170836", userId="studentmember")
			session.save(m)
			m
		}

		val relationship = transactional { tx =>
			val relationship = ExternalStudentRelationship("Professor A Tutor", StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee"), student)
			session.save(relationship)
			relationship
		}

		val createMeetingRecordCommand = new CreateMeetingRecordCommand(creator, relationship, false)
		createMeetingRecordCommand.title = "Title"
		createMeetingRecordCommand.format = FaceToFace
		createMeetingRecordCommand.meetingDateTime  = dateTime(3903, DateTimeConstants.MARCH) // it's the future
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
