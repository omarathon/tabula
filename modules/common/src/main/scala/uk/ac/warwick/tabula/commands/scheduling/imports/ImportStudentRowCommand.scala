package uk.ac.warwick.tabula.commands.scheduling.imports

import java.sql.ResultSet

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.{SitsStudentRow, PropertyCopying, ImportCommandFactory}
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.userlookup.User

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

	// these properties are from membership but may be overwritten by the SITS data (working theory)
	this.nationality = resultSet.getString("nationality")
	this.mobileNumber = resultSet.getString("mobile_number")

	val row = new SitsStudentRow(resultSet)
	this.deceased = row.deceased

	override def applyInternal(): Member = {
		transactional() {
			// set appropriate disability object iff a non-null code is retrieved - I <3 scala options
			profileService.getDisability(row.disabilityCode).foreach(this.disability = _)

				val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

				logger.debug("Importing student member " + universityId + " into " + memberExisting)

				val (isTransient, member) = memberExisting match {
					case Some(m: StudentMember) => (false, m)
					case Some(m: OtherMember) =>
						// TAB-692 delete the existing member, then return a brand new one
						// TAB-2188
						logger.info(s"Deleting $m while importing $universityId")
						memberDao.delete(m)
						(true, new StudentMember(universityId))
					case Some(m: ApplicantMember) =>
						// TAB-692 delete the existing member, then return a brand new one
						// TAB-2188
						logger.info(s"Deleting $m while importing $universityId")
						memberDao.delete(m)
						(true, new StudentMember(universityId))
					case Some(m) => throw new IllegalStateException("Tried to convert " + m + " into a student!")
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
			|| (member.tier4VisaRequirement.booleanValue() != tier4VisaRequirement))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)
			member.tier4VisaRequirement = tier4VisaRequirement
			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber", "disability", "deceased"
	)

	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}

trait ImportStudentRowCommandState extends StudentProperties
