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
import scala.reflect.BeanProperty
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

abstract class ImportSingleMemberCommand extends Command[Member] with Logging with Daoisms
	with MemberProperties  {
	
	PermissionCheck(Permission.ImportSystemData())
	
	var memberDao = Wire.auto[MemberDao]
	var fileDao = Wire.auto[FileDao]
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]	
	
	// A couple of intermediate properties that will be transformed later
	@BeanProperty var photoBlob: Blob = _
	@BeanProperty var homeDepartmentCode: String = _
	//@BeanProperty var studyDepartmentCode: String = _
	
	def this(rs: ResultSet) {
		this()
		
		this.universityId = rs.getString("university_id")
		this.userId = rs.getString("user_code")
		this.userType = MemberUserType.fromCode(rs.getString("group_ctg"))
		
		this.firstName = rs.getString("preferred_forename")
		this.lastName = rs.getString("family_name")
		this.email = rs.getString("email_address")
		this.title = rs.getString("title")
		this.fullFirstName = rs.getString("forenames")
		this.gender = Gender.fromCode(rs.getString("gender"))
		this.nationality = rs.getString("nationality")
		this.homeEmail = rs.getString("alternative_email_address")
		this.mobileNumber = rs.getString("mobile_number")
		this.photoBlob = rs.getBlob("photo")
				
		this.inUseFlag = rs.getString("in_use_flag")
		this.inactivationDate = toLocalDate(rs.getDate("date_of_inactivation"))
		this.groupName = rs.getString("group_name")
			
		this.homeDepartmentCode = rs.getString("home_department_code")
		this.dateOfBirth = toLocalDate(rs.getDate("date_of_birth"))
	}
	
	protected def toLocalDate(date: Date) = {
		if (date == null) {
			null
		} else {
			new LocalDate(date)
		}
	}
	
	protected def toAcademicYear(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			AcademicYear.parse(code)
		}
	}
	
	def applyForTesting = applyInternal
		
	/* Basic properties are those that use primitive types + String + DateTime etc, so can be updated with a simple equality check and setter */
	protected def copyBasicProperties(properties: Set[String], commandBean: BeanWrapper, memberBean: BeanWrapper) = {
		// Transform the set of properties to a set of booleans saying whether the value has changed
		val changedProperties = for (property <- properties) yield {
			val oldValue = memberBean.getPropertyValue(property)
			val newValue = commandBean.getPropertyValue(property)
			
			logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)
			
			// null == null in Scala so this is safe for unset values
			if (oldValue != newValue) {
				logger.debug("Detected property change; setting value")
				
				memberBean.setPropertyValue(property, newValue)
				true
			} else false
		}
		
		// Fold the set of booleans left with an || of false; this uses foldLeft rather than reduceLeft to handle the empty set
		changedProperties.foldLeft(false)(_ || _)
	}
	
	private def copyPhoto(property: String, blob: Blob, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: FileAttachment => value
		}
		
		val blobEmpty = (blob == null || blob.length == 0)
		
		logger.debug("Property " + property + ": " + oldValue + " -> " + blob)
		
		if (oldValue == null && blobEmpty) false
		else if (oldValue == null) {
			// From no photo to having a photo
			memberBean.setPropertyValue(property, toPhoto(blob))
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
			if (shaHash(oldValue.dataStream) == shaHash(blob.getBinaryStream)) false
			else {
				memberBean.setPropertyValue(property, toPhoto(blob))
				true
			}
		}
	}
	
	private def copyDepartment(property: String, departmentCode: String, memberBean: BeanWrapper) = {
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
		"userId", "firstName", "lastName", "email", "title", "fullFirstName", "userType", "gender",
		"nationality", "homeEmail", "mobileNumber", "inUseFlag", "inactivationDate", "groupName",
		"dateOfBirth"
	)
	
	// We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
	protected def copyMemberProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicMemberProperties, commandBean, memberBean) |
		copyPhoto("photo", photoBlob, memberBean) |
		copyDepartment("homeDepartment", homeDepartmentCode, memberBean)
	

	private def toPhoto(photoBlob: Blob) = {
		val photo = new FileAttachment
		photo.name = universityId + ".jpg"
		photo.uploadedData = photoBlob.getBinaryStream
		photo.uploadedDataLength = photoBlob.length
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
	
	def describe(d: Description) = d.property("universityId" -> universityId)

}
