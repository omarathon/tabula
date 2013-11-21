package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import scala.collection.JavaConverters.asScalaBufferConverter
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import ImportMemberHelpers.toAcademicYear
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.{Daoisms, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{ModeOfAttendance, ModuleRegistrationStatus, StudentCourseDetails, StudentCourseYearDetails, StudentCourseYearProperties}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{ImportRowTracker, PropertyCopying}
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentCourseYearKey

class ImportStudentCourseYearCommand(resultSet: ResultSet, importRowTracker: ImportRowTracker)
	extends Command[StudentCourseYearDetails] with Logging with Daoisms
	with StudentCourseYearProperties with Unaudited with PropertyCopying {
	import ImportMemberHelpers._

	implicit val rs = resultSet

	var modeOfAttendanceImporter = Wire.auto[ModeOfAttendanceImporter]
	var profileService = Wire.auto[ProfileService]
	var studentCourseYearDetailsDao = Wire.auto[StudentCourseYearDetailsDao]

	// A few intermediate properties that will be transformed later
	var enrolmentDepartmentCode: String = _
	var enrolmentStatusCode: String = _
	var modeOfAttendanceCode: String = _
	var academicYearString: String = _
	var moduleRegistrationStatusCode: String = _

	// This needs to be assigned before apply is called.
	// (can't be in the constructor because it's not yet created then)
	// TODO - use promises to make sure it gets assigned
	var studentCourseDetails: StudentCourseDetails = _

	this.yearOfStudy = rs.getInt("year_of_study")
	//this.fundingSource = rs.getString("funding_source")
	this.sceSequenceNumber = rs.getInt("sce_sequence_number")

	this.enrolmentDepartmentCode = rs.getString("enrolment_department_code")
	this.enrolmentStatusCode = rs.getString("enrolment_status_code")
	this.modeOfAttendanceCode = rs.getString("mode_of_attendance_code")
	this.academicYearString = rs.getString("sce_academic_year")
	this.moduleRegistrationStatusCode = rs.getString("mod_reg_status")

	override def applyInternal(): StudentCourseYearDetails = {
		transactional() {

			val studentCourseYearDetailsExisting = studentCourseYearDetailsDao.getBySceKeyStaleOrFresh(
				studentCourseDetails,
				sceSequenceNumber)

			logger.debug("Importing student course details for " + studentCourseDetails.scjCode + ", " + sceSequenceNumber)

			val commandBean = new BeanWrapperImpl(this)

			val (isTransient, studentCourseYearDetails) = studentCourseYearDetailsExisting match {
				case Some(studentCourseYearDetails: StudentCourseYearDetails) => (false, studentCourseYearDetails)
				case _ => (true, new StudentCourseYearDetails(studentCourseDetails, sceSequenceNumber,AcademicYear.parse(academicYearString)))
			}
			val studentCourseYearDetailsBean = new BeanWrapperImpl(studentCourseYearDetails)

			moduleRegistrationStatus = ModuleRegistrationStatus.fromCode(moduleRegistrationStatusCode)

			val hasChanged = copyStudentCourseYearProperties(commandBean, studentCourseYearDetailsBean) | markAsSeenInSits(studentCourseYearDetailsBean)

			if (isTransient || hasChanged) {
				logger.debug("Saving changes for " + studentCourseYearDetails)

				if (studentCourseDetails.latestStudentCourseYearDetails == null ||
					// need to include fresh or stale since this might be a row which was deleted but has been re-instated
					studentCourseDetails.freshOrStaleStudentCourseYearDetails.forall { _ <= studentCourseYearDetails }) {
					studentCourseDetails.latestStudentCourseYearDetails = studentCourseYearDetails
				}

				studentCourseYearDetails.lastUpdatedDate = DateTime.now
				studentCourseYearDetailsDao.saveOrUpdate(studentCourseYearDetails)
			}

			val key = new StudentCourseYearKey(studentCourseYearDetails.studentCourseDetails.scjCode, studentCourseYearDetails.sceSequenceNumber)
			importRowTracker.studentCourseYearDetailsSeen.add(key)

			studentCourseYearDetails
		}
	}

	private val basicStudentCourseYearProperties = Set(
		"yearOfStudy"
	)

	private def copyStudentCourseYearProperties(commandBean: BeanWrapper, studentCourseYearBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseYearProperties, commandBean, studentCourseYearBean) |
		copyObjectProperty("enrolmentDepartment", enrolmentDepartmentCode, studentCourseYearBean, toDepartment(enrolmentDepartmentCode)) |
		copyObjectProperty("enrolmentStatus", enrolmentStatusCode, studentCourseYearBean, toSitsStatus(enrolmentStatusCode)) |
		copyModeOfAttendance(modeOfAttendanceCode, studentCourseYearBean) |
		copyModuleRegistrationStatus(moduleRegistrationStatusCode, studentCourseYearBean)|
		copyAcademicYear("academicYear", academicYearString, studentCourseYearBean)
	}

	private def copyModeOfAttendance(code: String, studentCourseYearBean: BeanWrapper) = {
		val property = "modeOfAttendance"
		val oldValue = studentCourseYearBean.getPropertyValue(property) match {
			case null => null
			case value: ModeOfAttendance => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no MOA to having an MOA
			studentCourseYearBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		} else if (code == null) {
			// User had an SPR status code but now doesn't
			studentCourseYearBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			studentCourseYearBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		}
	}

	def copyModuleRegistrationStatus(code: String, destinationBean: BeanWrapper) = {
		val property = "moduleRegistrationStatus"
		val oldValue = destinationBean.getPropertyValue(property)
		val newValue = ModuleRegistrationStatus.fromCode(code)

		logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)

		// null == null in Scala so this is safe for unset values
		if (oldValue != newValue) {
			logger.debug("Detected property change for " + property + " (" + oldValue + " -> " + newValue + "); setting value")

			destinationBean.setPropertyValue(property, newValue)
			true
		}
		else false
	}

	private def copyAcademicYear(property: String, acYearString: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case value: AcademicYear => value
			case _ => null
		}

		val newValue = AcademicYear.parse(acYearString)

		if (oldValue == null && acYearString == null) false
		else if (oldValue == null) {
			// From no academic year to having an academic year
			memberBean.setPropertyValue(property, toAcademicYear(acYearString))
			true
		} else if (acYearString == null) {
			// Record had an academic year but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue == newValue) {
			false
		} else {
			memberBean.setPropertyValue(property, toAcademicYear(acYearString))
			true
		}
	}

	private def toModeOfAttendance(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			modeOfAttendanceImporter.getModeOfAttendanceForCode(code).orNull
		}
	}

	override def describe(d: Description) = d.property("scjCode" -> studentCourseDetails.scjCode).property("sceSequenceNumber" -> sceSequenceNumber)
}
