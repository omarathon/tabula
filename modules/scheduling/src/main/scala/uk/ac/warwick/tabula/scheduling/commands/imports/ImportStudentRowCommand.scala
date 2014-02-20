package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{PropertyCopying, ImportCommandFactory}
import uk.ac.warwick.tabula.scheduling.services._
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportMemberHelpers._
import scala.Some
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation

object ImportStudentRowCommand {
	def apply(
		member: MembershipInformation,
		ssoUser: User,
		resultSet: ResultSet,
		importCommandFactory: ImportCommandFactory) = {
			new ImportStudentRowCommandInternal(member, ssoUser, resultSet, importCommandFactory)
				with Command[Member]
				with AutowiringProfileServiceComponent
				with AutowiringTier4RequirementImporterComponent
				with AutowiringModeOfAttendanceImporterComponent
				with Unaudited
			}
}

/*
 * ImportStudentRowCommand takes an importCommandFactory which enables sub-commands to be created
  * while enabling testing without auto-wiring.
 */
class ImportStudentRowCommandInternal(
			 val member: MembershipInformation,
			 val ssoUser: User,
			 val resultSet: ResultSet,
			 var importCommandFactory: ImportCommandFactory
		 ) extends ImportMemberCommand(member, ssoUser, Some(resultSet))
				with Describable[Member]
				with ImportStudentRowCommandState
				with PropertyCopying
				with Logging {

	self: ProfileServiceComponent with Tier4RequirementImporterComponent with ModeOfAttendanceImporterComponent  =>

	// these properties are from membership but may be overwritten by the SITS row??
	this.nationality = resultSet.getString("nationality")
	this.mobileNumber = resultSet.getString("mobile_number")

	val row = new SitsStudentRow(resultSet)

	override def applyInternal(): Member = {
		transactional() {
			// set appropriate disability object iff a non-null code is retrieved - I <3 scala options
			profileService.getDisability(row.disabilityCode).foreach(this.disability = _)

			val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

			logger.debug("Importing member " + universityId + " into " + memberExisting)

			val (isTransient, member) = memberExisting match {
				case Some(member: StudentMember) => (false, member)
				case Some(member: OtherMember) => {
					// TAB-692 delete the existing member, then return a brand new one
					memberDao.delete(member)
					(true, new StudentMember(universityId))
				}
				case Some(member) => throw new IllegalStateException("Tried to convert " + member + " into a student!")
				case _ => (true, new StudentMember(universityId))
			}

			if (!importCommandFactory.rowTracker.universityIdsSeen.contains(member.universityId)) {
				saveStudentDetails(isTransient, member)
			}

			val studentCourseDetails = importCommandFactory.createImportStudentCourseCommand(row, member).apply()
			member.attachStudentCourseDetails(studentCourseDetails)

			importCommandFactory.rowTracker.universityIdsSeen.add(member.universityId)

			member
		}
	}

	private def saveStudentDetails(isTransient: Boolean, member: StudentMember) {
		// There are multiple rows returned by the SQL per student; only import non-course details if we haven't already
		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		val tier4VisaRequirement = tier4RequirementImporter.hasTier4Requirement(universityId)

		// We intentionally use single pipes rather than double here - we want all statements to be evaluated
		val hasChanged = (copyMemberProperties(commandBean, memberBean)
			| copyStudentProperties(commandBean, memberBean)
			| markAsSeenInSits(memberBean)
			|| (member.tier4VisaRequirement != tier4VisaRequirement))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)
			member.tier4VisaRequirement = tier4VisaRequirement
			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber", "disability"
	)

	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}

trait ImportStudentRowCommandState extends StudentProperties

// SitsStudentRow contains the data from the result set for the SITS query.  (It doesn't include member properties)
class SitsStudentRow(resultSet: ResultSet)
	extends SitsStudentRowCourseDetails
	with SitsStudentRowYearDetails {

	def rs = resultSet

	val disabilityCode = resultSet.getString("disability")
}

// this trait holds data from the result set which will be used by ImportStudentCourseCommand to create
// StudentCourseDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowCourseDetails
	extends StudentCoursePropertiesFromSits
{
	def rs: ResultSet

	var routeCode = rs.getString("route_code")
	var courseCode = rs.getString("course_code")
	var sprStatusCode = rs.getString("spr_status_code")
	var scjStatusCode = rs.getString("scj_status_code")
	var departmentCode = rs.getString("department_code")
	var awardCode = rs.getString("award_code")

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	var tutorUniId = rs.getString("spr_tutor1")

	// this is the key and is not included in StudentCourseProperties, so just storing it in a var:
	var scjCode: String = rs.getString("scj_code")

	// now grab data from the result set into properties
	this.mostSignificant = rs.getString("most_signif_indicator") match {
		case "Y" | "y" => true
		case _ => false
	}

	this.sprCode = rs.getString("spr_code")
	this.beginDate = toLocalDate(rs.getDate("begin_date"))
	this.endDate = toLocalDate(rs.getDate("end_date"))
	this.expectedEndDate = toLocalDate(rs.getDate("expected_end_date"))
	this.courseYearLength = rs.getString("course_year_length")
	this.levelCode = rs.getString("level_code")
}

// this trait holds data from the result set which will be used by ImportStudentCourseYearCommand to create
// StudentCourseYearDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowYearDetails extends StudentCourseYearPropertiesFromSits {
	def rs: ResultSet

	var enrolmentDepartmentCode: String = rs.getString("enrolment_department_code")
	var enrolmentStatusCode: String = rs.getString("enrolment_status_code")
	var modeOfAttendanceCode: String = rs.getString("mode_of_attendance_code")
	var academicYearString: String = rs.getString("sce_academic_year")
	var moduleRegistrationStatusCode: String = rs.getString("mod_reg_status")

	this.yearOfStudy = rs.getInt("year_of_study")
	//this.fundingSource = rs.getString("funding_source")
	this.sceSequenceNumber = rs.getInt("sce_sequence_number")
}