package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.OtherMember
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User

class ImportStudentRowCommand(member: MembershipInformation, ssoUser: User, resultSet: ResultSet, importStudentCourseCommand: ImportStudentCourseCommand)
	extends ImportMemberCommand(member, ssoUser, Some(resultSet))
	with Logging with Daoisms
	with StudentProperties with Unaudited with PropertyCopying {

	import ImportMemberHelpers._

	implicit val rs = resultSet

	var modeOfAttendanceImporter = Wire.auto[ModeOfAttendanceImporter]
	var profileService = Wire.auto[ProfileService]

	this.nationality = rs.getString("nationality")
	this.mobileNumber = rs.getString("mobile_number")

	override def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityId(universityId)

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

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		val hasChanged = copyMemberProperties(commandBean, memberBean) | copyStudentProperties(commandBean, memberBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		importStudentCourseCommand.stuMem = member
		val studentCourseDetails = importStudentCourseCommand.apply()

		// apply above will take care of the db.  This brings the in-memory data up to speed:
		member.attachStudentCourseDetails(studentCourseDetails)

		member
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber"
	)

	// We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}
