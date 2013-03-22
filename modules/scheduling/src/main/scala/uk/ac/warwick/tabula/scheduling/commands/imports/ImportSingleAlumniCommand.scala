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
import scala.beans.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.BeanWrapper
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.Closeables._
import java.io.InputStream
import org.apache.commons.codec.digest.DigestUtils
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.OtherMember

class ImportSingleAlumniCommand(member: MembershipInformation, ssoUser: User, rs: ResultSet) extends ImportSingleMemberCommand(member, ssoUser, rs)
	with Logging with Daoisms with AlumniProperties with Unaudited {
	import ImportMemberHelpers._
	
	// any initialisation code specific to alumni (e.g. setting alumni properties) can go here
	
	def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityId(universityId)
		
		logger.debug("Importing member " + universityId + " into " + memberExisting)
		
		val isTransient = !memberExisting.isDefined
		val member = memberExisting getOrElse(new OtherMember(universityId))
		
		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)
		
		// We intentionally use a single pipe rather than a double pipe here - we want both statements to be evaluated
		val hasChanged = copyMemberProperties(commandBean, memberBean) | copyAlumniProperties(commandBean, memberBean)
			
		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)
			
			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
		
		member
	}
		
	private val basicAlumniProperties: Set[String] = Set()
	
	private def copyAlumniProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicAlumniProperties, commandBean, memberBean)
		
	
	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "alumni")


}