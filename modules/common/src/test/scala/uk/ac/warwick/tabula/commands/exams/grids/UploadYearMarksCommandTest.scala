package uk.ac.warwick.tabula.commands.exams.grids

import java.io.InputStream

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment}
import uk.ac.warwick.tabula.data.{FileDao, StudentCourseYearDetailsDao, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.services.coursework.docconversion.{YearMarkItem, YearMarksExtractor, YearMarksExtractorComponent}
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}

class UploadYearMarksCommandTest extends TestBase with Mockito {

	val fileDao: FileDao = smartMock[FileDao]
	fileDao.getData(null) returns Option(createTemporaryFile())
	val maintenanceMode = smartMock[MaintenanceModeService]
	maintenanceMode.enabled returns false

	trait BindFixture {
		val thisDepartment = Fixtures.department("this")
		val otherDepartment = Fixtures.department("other")
		val validStudent = Fixtures.student("1234")
		validStudent.mostSignificantCourse.latestStudentCourseYearDetails.enrolmentDepartment = thisDepartment
		val invalidStudent = Fixtures.student("3456")
		invalidStudent.mostSignificantCourse.latestStudentCourseYearDetails.enrolmentDepartment = otherDepartment

		val command = new UploadYearMarksCommandBindListener with UploadYearMarksCommandRequest
			with UploadYearMarksCommandState with YearMarksExtractorComponent	with StudentCourseYearDetailsDaoComponent {
			val studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
			val department: Department = thisDepartment
			val academicYear: AcademicYear = AcademicYear(2015)
			val yearMarksExtractor: YearMarksExtractor = smartMock[YearMarksExtractor]
		}
	}

	@Test
	def checkInvalidFileName(): Unit = {
		new BindFixture {
			val badFile = new UploadedFile
			val badAttachment = new FileAttachment
			badAttachment.name = "file.txt"
			badFile.attached.add(badAttachment)
			command.file = badFile
			val errors = new BindException(command, "command")
			command.onBind(errors)
			errors.hasFieldErrors("file") should be {true}
		}
	}

	@Test
	def checkValidFileName(): Unit = {
		new BindFixture {
			val file = new UploadedFile
			file.maintenanceMode = maintenanceMode
			val attachment = new FileAttachment
			attachment.fileDao = fileDao
			attachment.name = "file.xlsx"
			file.attached.add(attachment)
			command.file = file

			command.yearMarksExtractor.readXSSFExcelFile(any[InputStream]) returns JArrayList(
				new YearMarkItem("123", "60")
			)

			command.studentCourseYearDetailsDao.findByUniversityIdAndAcademicYear(any[Seq[(String, AcademicYear)]]) returns Map()
			command.studentCourseYearDetailsDao.findByScjCodeAndAcademicYear(any[Seq[(String, AcademicYear)]]) returns Map()

			val errors = new BindException(command, "command")
			command.onBind(errors)
			errors.hasFieldErrors("file") should be {false}
			command.marks.size() should be (1)
		}
	}

	@Test
	def processMarks(): Unit = {
		new BindFixture {
			val file = new UploadedFile
			file.maintenanceMode = maintenanceMode
			command.file = file

			command.marks.addAll(JArrayList(
				new YearMarkItem("2345", "60"), // Invalid Uni Id
				new YearMarkItem("2345/1", "60"), // Invalid Scj code
				new YearMarkItem(validStudent.universityId, "60.1.2", "14/15"), // Invalid Mark (bad format)
				new YearMarkItem(validStudent.universityId, "-1", "14/15"), // Invalid Mark (too small)
				new YearMarkItem(validStudent.universityId, "60.6"), // Fine, but missing academic year
				new YearMarkItem(validStudent.universityId, "60.6", "12/13"), // No such SCYD for this year
				new YearMarkItem(validStudent.mostSignificantCourse.scjCode, "60.667", "13/14"), // All good
				new YearMarkItem(invalidStudent.mostSignificantCourse.scjCode, "60.667", "13/14") // Wrong department
			))

			command.studentCourseYearDetailsDao.findByUniversityIdAndAcademicYear(any[Seq[(String, AcademicYear)]]) returns Map(
				(validStudent.universityId, AcademicYear(2013)) -> validStudent.mostSignificantCourse.latestStudentCourseYearDetails,
				(validStudent.universityId, AcademicYear(2014)) -> validStudent.mostSignificantCourse.latestStudentCourseYearDetails,
				(validStudent.universityId, AcademicYear(2015)) -> validStudent.mostSignificantCourse.latestStudentCourseYearDetails
			)
			command.studentCourseYearDetailsDao.findByScjCodeAndAcademicYear(any[Seq[(String, AcademicYear)]]) returns Map(
				(validStudent.mostSignificantCourse.scjCode, AcademicYear(2013)) -> validStudent.mostSignificantCourse.latestStudentCourseYearDetails,
				(validStudent.mostSignificantCourse.scjCode, AcademicYear(2014)) -> validStudent.mostSignificantCourse.latestStudentCourseYearDetails,
				(validStudent.mostSignificantCourse.scjCode, AcademicYear(2015)) -> validStudent.mostSignificantCourse.latestStudentCourseYearDetails,
				(invalidStudent.mostSignificantCourse.scjCode, AcademicYear(2013)) -> invalidStudent.mostSignificantCourse.latestStudentCourseYearDetails
			)

			val errors = new BindException(command, "command")
			command.onBind(errors)
			command.processedYearMarks.size should be (8)
			command.processedYearMarks.count(_.errors.nonEmpty) should be (6)
			val validWithMissingAcademicYear = command.processedYearMarks.find(i => i.scjCode == validStudent.mostSignificantCourse.scjCode && i.academicYear == command.academicYear)
			validWithMissingAcademicYear.isDefined should be {true}
			validWithMissingAcademicYear.get.errors.isEmpty should be {true}
			val valid = command.processedYearMarks.find(i => i.scjCode == validStudent.mostSignificantCourse.scjCode && i.academicYear == AcademicYear(2013))
			valid.isDefined should be {true}
			valid.get.errors.isEmpty should be {true}
			valid.get.mark.doubleValue should be (60.7) // Check rounding
		}
	}

}
