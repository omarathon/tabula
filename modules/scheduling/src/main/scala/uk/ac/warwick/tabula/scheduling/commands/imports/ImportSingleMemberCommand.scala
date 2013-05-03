package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.AlumniProperties
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.MemberProperties
import uk.ac.warwick.tabula.data.model.StaffProperties
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import java.sql.ResultSet
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.tabula.data.model.MemberUserType
import java.sql.Blob
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.Transactions._
import java.sql.Date
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.BeanWrapper
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.Closeables._
import java.io.InputStream
import org.apache.commons.codec.digest.DigestUtils
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.commands.Unaudited
import java.io.ByteArrayInputStream
import java.sql.ResultSetMetaData
import uk.ac.warwick.userlookup.User
import org.apache.commons.lang3.text.WordUtils
import scala.util.matching.Regex
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import language.implicitConversions

abstract class ImportSingleMemberCommand extends Command[Member] with Logging with Daoisms
	with MemberProperties with Unaudited with PropertyCopying {
	import ImportMemberHelpers._

	PermissionCheck(Permissions.ImportSystemData)

	var memberDao = Wire.auto[MemberDao]
	var fileDao = Wire.auto[FileDao]
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

	// A couple of intermediate properties that will be transformed later
	var photoOption: Option[Array[Byte]] = _
	var homeDepartmentCode: String = _
	//var studyDepartmentCode: String = _

	def this(mac: MembershipInformation, ssoUser: User, rs: ResultSet) {
		this()

		implicit val resultSet = rs
		implicit val metadata = rs.getMetaData

		val member = mac.member

		this.universityId = oneOf(member.universityId, optString("university_id")).get
		this.userId = member.usercode

		this.userType = member.userType

		this.title = oneOf(member.title, optString("title")) map { WordUtils.capitalizeFully(_).trim() } getOrElse("")
		this.firstName = oneOf(member.preferredForenames, optString("preferred_forename"), ssoUser.getFirstName) map { formatForename(_, ssoUser.getFirstName) } getOrElse("")
		this.fullFirstName = oneOf(optString("forenames"), ssoUser.getFirstName) map { formatForename(_, ssoUser.getFirstName) } getOrElse("")
		this.lastName = oneOf(member.preferredSurname, optString("family_name"), ssoUser.getLastName) map { formatSurname(_, ssoUser.getLastName) } getOrElse("")

		this.email = (oneOf(member.email, optString("email_address"), ssoUser.getEmail).orNull)
		this.homeEmail = (oneOf(member.alternativeEmailAddress, optString("alternative_email_address")).orNull)

		this.gender = (oneOf(member.gender, optString("gender") map { Gender.fromCode(_) }).orNull)
		this.photoOption = mac.photo

		this.jobTitle = member.position
		this.phoneNumber = member.phoneNumber

		this.inUseFlag = rs.getString("in_use_flag")
		this.groupName = member.targetGroup
		this.inactivationDate = member.endDate

		this.homeDepartmentCode = (oneOf(member.departmentCode, optString("home_department_code"), ssoUser.getDepartmentCode).orNull)
		this.dateOfBirth = (oneOf(member.dateOfBirth, optLocalDate("date_of_birth")).orNull)
	}

	private def copyPhoto(property: String, photoOption: Option[Array[Byte]], memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: FileAttachment => value
		}

		val blobEmpty = !photoOption.isDefined || photoOption.get.length == 0

		logger.debug("Property " + property + ": " + oldValue + " -> " + photoOption)

		if (oldValue == null && blobEmpty) false
		else if (oldValue == null) {
			// From no photo to having a photo
			memberBean.setPropertyValue(property, toPhoto(photoOption.get))
			true
		} else if (blobEmpty) {
			// User had a photo but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else {
			def shaHash(is: InputStream) =
				if (is == null) null
				else closeThis(is) { is => DigestUtils.shaHex(is) }

			// Need to check whether the existing photo matches the new photo
			if (shaHash(oldValue.dataStream) == shaHash(new ByteArrayInputStream(photoOption.get))) false
			else {
				memberBean.setPropertyValue(property, toPhoto(photoOption.get))
				true
			}
		}
	}

	protected def copyDepartment(property: String, departmentCode: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: Department => value
		}

		if (oldValue == null && departmentCode == null) false
		else if (oldValue == null) {
			// From no department to having a department
			memberBean.setPropertyValue(property, toDepartment(departmentCode))
			true
		} else if (departmentCode == null) {
			// User had a department but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == departmentCode.toLowerCase) {
			false
		}	else {
			memberBean.setPropertyValue(property, toDepartment(departmentCode))
			true
		}
	}

	private val basicMemberProperties = Set(
		"userId", "firstName", "lastName", "email", "homeEmail", "title", "fullFirstName", "userType", "gender",
		"inUseFlag", "inactivationDate", "groupName", "dateOfBirth", "jobTitle", "phoneNumber"
	)

	// We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
	protected def copyMemberProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicMemberProperties, commandBean, memberBean) |
		copyPhoto("photo", photoOption, memberBean) |
		copyDepartment("homeDepartment", homeDepartmentCode, memberBean)

	private def toPhoto(bytes: Array[Byte]) = {
		val photo = new FileAttachment
		photo.name = universityId + ".jpg"
		photo.uploadedData = () => new ByteArrayInputStream(bytes)
		photo.uploadedDataLength = bytes.length
		fileDao.savePermanent(photo)
		photo
	}

	private def toDepartment(departmentCode: String) = {
		if (departmentCode == null || departmentCode == "") {
			null
		} else {
			moduleAndDepartmentService.getDepartmentByCode(departmentCode.toLowerCase).getOrElse(null)
		}
	}

	override def describe(d: Description) = d.property("universityId" -> universityId)

}

object ImportMemberHelpers {

	implicit def opt[A](value: A) = Option(value)

	/** Return the first Option that has a value, else None. */
	def oneOf[A](options: Option[A]*) = options.flatten.headOption

	def optString(columnName: String)(implicit rs: ResultSet, metadata: ResultSetMetaData): Option[String] =
		if (hasColumn(columnName)) Some(rs.getString(columnName))
		else None

	def optLocalDate(columnName: String)(implicit rs: ResultSet, metadata: ResultSetMetaData): Option[LocalDate] =
		if (hasColumn(columnName)) Some(rs.getDate(columnName)) map { new LocalDate(_) }
		else None

	def hasColumn(columnName: String)(implicit rs: ResultSet, metadata: ResultSetMetaData) = {
		val cols = for (col <- 1 to metadata.getColumnCount) yield columnName == metadata.getColumnName(col)
		cols.exists(b => b)
	}

	def toLocalDate(date: Date) = {
		if (date == null) {
			null
		} else {
			new LocalDate(date)
		}
	}

	def toAcademicYear(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			AcademicYear.parse(code)
		}
	}

	private val CapitaliseForenamePattern = """(?:(\p{Lu})(\p{L}*)([^\p{L}]?))""".r

	def formatForename(name: String, suggested: String = null): String = {
		if (name.equalsIgnoreCase(suggested)) {
			// Our suggested capitalisation from SSO was correct
			suggested
		} else {
			CapitaliseForenamePattern.replaceAllIn(name, { m: Regex.Match =>
				m.group(1).toUpperCase() + m.group(2).toLowerCase() + m.group(3)
			}).trim()
		}
	}

	private val CapitaliseSurnamePattern = """(?:((\p{Lu})(\p{L}*))([^\p{L}]?))""".r
	private val WholeWordGroup = 1
	private val FirstLetterGroup = 2
	private val RemainingLettersGroup = 3
	private val SeparatorGroup = 4

	def formatSurname(name: String, suggested: String = null): String = {
		if (name.equalsIgnoreCase(suggested)) {
			// Our suggested capitalisation from SSO was correct
			suggested
		} else {
			/*
			 * Conventions:
			 *
			 * von - do not capitalise de La - capitalise second particle O', Mc,
			 * Mac, M' - always capitalise
			 */

			CapitaliseSurnamePattern.replaceAllIn(name, { m: Regex.Match =>
				val wholeWord = m.group(WholeWordGroup).toUpperCase()
				val first = m.group(FirstLetterGroup).toUpperCase()
				val remainder = m.group(RemainingLettersGroup).toLowerCase()
				val separator = m.group(SeparatorGroup)

				if (wholeWord.startsWith("MC") && wholeWord.length() > 2) {
					// Capitalise the first letter of the remainder
					first +
						remainder.substring(0, 1) +
						remainder.substring(1, 2).toUpperCase() +
						remainder.substring(2) +
						separator
				} else if (wholeWord.startsWith("MAC") && wholeWord.length() > 3) {
					// Capitalise the first letter of the remainder
					first +
						remainder.substring(0, 2) +
						remainder.substring(2, 3).toUpperCase() +
						remainder.substring(3) +
						separator
				} else if (wholeWord.equals("VON") || wholeWord.equals("D") || wholeWord.equals("DE") || wholeWord.equals("DI")) {
					// Special case - lowercase the first word
					first.toLowerCase() + remainder + separator
				} else {
					first + remainder + separator
				}
			}).trim()
		}
	}
}