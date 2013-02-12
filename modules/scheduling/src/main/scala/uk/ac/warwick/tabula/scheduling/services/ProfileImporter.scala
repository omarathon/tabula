package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import java.sql.Types
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap
import scala.util.matching.Regex
import org.apache.commons.lang3.text.WordUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Address
import uk.ac.warwick.tabula.data.model.AddressType
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.NextOfKin
import uk.ac.warwick.userlookup.User
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleMemberCommand
import uk.ac.warwick.tabula.data.model.Staff
import uk.ac.warwick.tabula.data.model.Student
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStaffCommand
import uk.ac.warwick.tabula.helpers.Logging

case class UserIdAndCategory(val userId: String, val category: String)

@Service
class ProfileImporter extends InitializingBean with Logging {
	import ProfileImporter._
	
	var ads = Wire[DataSource]("academicDataStore")
	
	var jdbc: NamedParameterJdbcTemplate = _
	
	override def afterPropertiesSet {
		jdbc = new NamedParameterJdbcTemplate(ads)
	}
	
	lazy val studentInformationQuery = new StudentInformationQuery(ads)
	lazy val staffInformationQuery = new StaffInformationQuery(ads)

	def getMemberDetails(userIdsAndCategories: Seq[UserIdAndCategory]): Seq[ImportSingleMemberCommand] = {
		val commands = for (userIdAndCategory <- userIdsAndCategories) yield {
			val userId = userIdAndCategory.userId
			val category = MemberUserType.fromCode(userIdAndCategory.category)
			logger.debug("Reading data for " + category.description + " member, " + userId)
			category match {
				case Student =>	studentInformationQuery.executeByNamedParam(Map("usercodes" -> userId)).toSeq
				case Staff =>	staffInformationQuery.executeByNamedParam(Map("usercodes" -> userId)).toSeq
				case _ => Seq()
			}
		}
		commands.flatten
	}
	
	lazy val allUserIdsAndCategoriesQuery = new AllUserIdsAndCategoriesQuery(ads)
	lazy val userIdsAndCategories: Seq[UserIdAndCategory] = allUserIdsAndCategoriesQuery.execute
	
	lazy val nextOfKinQuery = new NextOfKinsQuery(ads)
	lazy val addressQuery = new AddressesQuery(ads)
	
	def getNextOfKins(member: Member): Seq[NextOfKin] = nextOfKinQuery.executeByNamedParam(Map(
	    "universityId" -> member.universityId))
	    
	def getAddresses(member: Member): Seq[Address] = addressQuery.executeByNamedParam(Map(
	    "universityId" -> member.universityId))
	
	def processNames(member: ImportSingleMemberCommand, users: Map[String, User]) = {
		val ssoUser = users(member.userId)
		
		member.title = WordUtils.capitalizeFully(Option(member.title).getOrElse("")).trim()
		member.firstName = formatForename(Option(member.firstName).getOrElse(""), Option(ssoUser.getFirstName()).getOrElse(""))
		member.fullFirstName = formatForename(Option(member.fullFirstName).getOrElse(""), Option(ssoUser.getFirstName()).getOrElse(""))
		member.lastName = formatSurname(Option(member.lastName).getOrElse(""), Option(ssoUser.getLastName()).getOrElse(""))
	  
		member
	}
}

object ProfileImporter {
  
	val GetAllUsersAndCategories = """
		select primary_user_code, group_ctg from member
		where primary_user_code is not null
		and preferred_email_address is not null
		and preferred_email_address != 'No Email'
		and in_use_flag = 'Active'
		"""
  
	val BasicInformationPrologue = """
	  	select
			m.university_id as university_id,
			m.title as title,
			m.preferred_forename as preferred_forename,
			m.forenames as forenames,
			m.family_name as family_name,
			m.gender as gender,
			nat.name as nationality,
			photo.photo as photo,
			m.in_use_flag as in_use_flag,
			m.date_of_inactivation as date_of_inactivation,
			m.group_name as group_name,
			m.department_code as home_department_code,
			m.preferred_email_address as email_address,
			m.home_email_address as alternative_email_address,
			m.mobile_phone_number as mobile_number,
			m.primary_user_code as user_code,
			m.date_of_birth as date_of_birth,
			m.group_ctg as group_ctg
		"""
		
	val BasicInformationEpilogue = """
		left outer join member_photo_details photo 
			on m.university_id = photo.university_id

		left outer join nationality nat 
			on m.nationality = nat.nationality_code
		
		where m.primary_user_code in (:usercodes)
		"""
		
	val GetStudentInformation = BasicInformationPrologue + """,
			study.spr_code as spr_code,
			study.sits_course_code as sits_course_code,
			levl.name as study_level,
			study.year_of_study as year_of_study,
			moa.name as mode_of_attendance,
			funding.name as source_of_funding,
			student_status.name as student_status,
			pos.name as programme_of_study,
			intended_award.name as intended_award,
			study.route_code as route_code,
			study.academic_year_code as academic_year_code,
			study.category as category,
			study.department as study_department,
			study.course_start_academic_yr as course_start_year,
			study.base_course_start_ayr as course_base_start_year,
			study.course_end_date as course_end_date,
			transfer.name as transfer_reason,
			details.next_of_kin as next_of_kin,
			details.begin_date as begin_date,
			details.end_date as end_date,
			details.expected_end_date as expected_end_date,
			fees.name as fee_status,
			domicile.name as domicile,
			entry_qual.name as highest_qualification_on_entry,
			last_institute.name as last_institute,
			last_school.name as last_school,
			details.year_commenced_degree as year_commenced_degree,
			enrolment_status as enrolment_status

		from member m
	
    		left outer join student_current_study_details study 
      			on m.university_id = study.university_id
    
			left outer join transfer_reasons transfer 
	  			on study.transfer_code_reason = transfer.code
    
    		left outer join source_of_funding funding 
	  			on study.source_of_funding = funding.sof_code
        
    		left outer join programme_of_study pos 
	  			on study.programme_of_study = pos.programme_of_study_code
    
    		left outer join student_programme_route programme 
	  			on study.spr_code = programme.spr_code
    
    		left outer join award intended_award 
	  			on programme.intended_award = intended_award.award_code
    
    		left outer join levl levl 
	  			on study.level_code = levl.level_code
    
    		left outer join status student_status 
	  			on study.student_status = student_status.status_code
	     
    		left outer join mode_of_attendance moa 
	  			on study.mode_of_attendance = moa.moa_code
    
    		left outer join student_personal_details details 
	  			on m.university_id = details.university_id
    
    		left outer join fee_status fees 
	  			on details.fee_status = fees.fee_status_code
	  
    		left outer join domicile domicile 
	  			on details.domicile_code = domicile.domicile_code
	  
    		left outer join qualification entry_qual 
	  			on details.highest_qualification_on_entry = entry_qual.qualification_code
	  
    		left outer join institute last_institute 
	  			on details.last_institute = last_institute.institute_code
	  
    		left outer join school last_school 
	  			on details.last_school = last_school.school_code
		""" + BasicInformationEpilogue
		
	val GetStaffInformation = BasicInformationPrologue + """,
			m.teaching_staff as teaching_staff
		from member m
		""" + BasicInformationEpilogue
	  
	val GetNextOfKins = """
		select 
	  		forenames, 
	  		family_name, 
	  		relationship, 
	  		address1, 
	  		address2, 
	  		address3, 
	  		address4, 
	  		address5, 
	  		postcode, 
	  		day_telephone,
	  		evening_telephone,
	  		email_address
	  	from next_of_kin
			where university_id = :universityId
		"""
	  
	val GetAddresses = """
		select 
	  		address1,
	  		address2,
	  		address3,
	  		address4,
	  		address5,
	  		postcode,
	  		telephone,
	  		type
	  	from address
			where university_id = :universityId
		"""
	  
	class AllUserIdsAndCategoriesQuery(ds: DataSource) extends MappingSqlQuery[UserIdAndCategory](ds, GetAllUsersAndCategories) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = UserIdAndCategory(rs.getString("primary_user_code"), rs.getString("group_ctg"))
	}
	  
	class StudentInformationQuery(ds: DataSource) extends MappingSqlQuery[ImportSingleStudentCommand](ds, GetStudentInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		compile()		
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStudentCommand(rs)
	}
	
	class StaffInformationQuery(ds: DataSource) extends MappingSqlQuery[ImportSingleStaffCommand](ds, GetStaffInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		compile()		
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStaffCommand(rs)
	}
	
	class NextOfKinsQuery(ds: DataSource) extends MappingSqlQuery[NextOfKin](ds, GetNextOfKins) {
		declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = {
			val kin = new NextOfKin
			
			kin.firstName = formatForename(rs.getString("forenames"))
			kin.lastName = formatSurname(rs.getString("family_name"))
			kin.relationship = WordUtils.capitalizeFully(Option(rs.getString("relationship")).getOrElse("")).trim()
			
			val address = new Address
			address.line1 = WordUtils.capitalizeFully(Option(rs.getString("address1")).getOrElse("")).trim()
			address.line2 = WordUtils.capitalizeFully(Option(rs.getString("address2")).getOrElse("")).trim()
			address.line3 = WordUtils.capitalizeFully(Option(rs.getString("address3")).getOrElse("")).trim()
			address.line4 = WordUtils.capitalizeFully(Option(rs.getString("address4")).getOrElse("")).trim()
			address.line5 = WordUtils.capitalizeFully(Option(rs.getString("address5")).getOrElse("")).trim()
			address.postcode = rs.getString("postcode")
			address.telephone = rs.getString("day_telephone")
			
			if (!address.isEmpty)
				kin.address = address
				
			kin.eveningPhone = rs.getString("evening_telephone")
			kin.email = rs.getString("email_address")
			
			kin
		}
	}
	
	class AddressesQuery(ds: DataSource) extends MappingSqlQuery[Address](ds, GetAddresses) {
		declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = {
			val address = new Address
			address.line1 = WordUtils.capitalizeFully(Option(rs.getString("address1")).getOrElse("")).trim()
			address.line2 = WordUtils.capitalizeFully(Option(rs.getString("address2")).getOrElse("")).trim()
			address.line3 = WordUtils.capitalizeFully(Option(rs.getString("address3")).getOrElse("")).trim()
			address.line4 = WordUtils.capitalizeFully(Option(rs.getString("address4")).getOrElse("")).trim()
			address.line5 = WordUtils.capitalizeFully(Option(rs.getString("address5")).getOrElse("")).trim()
			address.postcode = rs.getString("postcode")
			address.telephone = rs.getString("telephone")
			
			address.addressType = AddressType.fromCode(rs.getString("type"))
			
			address
		}
	}
	  
	private val CapitaliseForenamePattern = """(?:(\p{Lu})(\p{L}*)([^\p{L}]?))""".r
	  
	private def formatForename(name: String, suggested: String = null): String = {
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
	
	private def formatSurname(name: String, suggested: String = null): String = {
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