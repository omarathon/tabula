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
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStaffCommand
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.remoting.rmi.RmiProxyFactoryBean
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Department
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.membership.MembershipInterfaceWrapper

case class MembershipInformation(val member: MembershipMember, val photo: Option[Array[Byte]])

@Service
class ProfileImporter extends Logging {
	import ProfileImporter._
	
	var ads = Wire[DataSource]("academicDataStore")
	var sits = Wire[DataSource]("sitsDataSource")
	var membership = Wire[DataSource]("membershipDataSource")
	var membershipInterface = Wire.auto[MembershipInterfaceWrapper]
	
	lazy val membershipByDepartmentQuery = new MembershipByDepartmentQuery(membership)
	lazy val membershipByUsercodeQuery = new MembershipByUsercodeQuery(membership)
	
	def studentInformationQuery(member: MembershipInformation, ssoUser: User) = new StudentInformationQuery(ads, member, ssoUser)
	def staffInformationQuery(member: MembershipInformation, ssoUser: User) = new StaffInformationQuery(sits, member, ssoUser)

	def getMemberDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportSingleMemberCommand] = {
		// TODO we could probably chunk this into 20 or 30 users at a time for the query, or even split by category and query all at once
		
		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(usercode)
			
			mac.member.userType match {
				case Student 		   => studentInformationQuery(mac, ssoUser).executeByNamedParam(Map("usercodes" -> usercode)).toSeq
				case Staff | Emeritus  => staffInformationQuery(mac, ssoUser).executeByNamedParam(Map("usercodes" -> usercode)).toSeq
				case _ => Seq()
			}
		}
	}
		
	def userIdsAndCategories(department: Department): Seq[MembershipInformation] = 
		membershipByDepartmentQuery.executeByNamedParam(Map("departmentCode" -> department.getCode.toUpperCase)).toSeq map { member =>
			MembershipInformation(member, Option(membershipInterface.getPhotoById(member.universityId)))
		}
	
	def userIdAndCategory(member: Member) =
		MembershipInformation(
			membershipByUsercodeQuery.executeByNamedParam(Map("usercodes" -> member.userId)).head, 
			Option(membershipInterface.getPhotoById(member.universityId))
		)
}

object ProfileImporter {
  
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
		
	val GetStaffInformation = """
		select
			prs.prs_udf1 as university_id,
			prs.prs_ttlc as title,
			prs.prs_fusd as preferred_forename,
			trim(prs.prs_fnm1 || ' ' || prs.prs_fnm2 || ' ' || prs.prs_fnm3) as forenames,
			prs.prs_surn as family_name,
			prs.prs_gend as gender,
			case prs.prs_iuse when 'Y' then 'Active' else 'Inactive' end as in_use_flag,
			prs.prs_dptc as home_department_code,
			prs.prs_emad as email_address,
			prs.prs_exid as user_code,
			prs.prs_dob as date_of_birth,
			
			case when prs.prs_psac is null then 'N' else 'Y' end as teaching_staff
		from ins_prs prs
			where prs.prs_exid in (:usercodes)
		"""
	  
	class StudentInformationQuery(ds: DataSource, member: MembershipInformation, ssoUser: User) extends MappingSqlQuery[ImportSingleStudentCommand](ds, GetStudentInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStudentCommand(member, ssoUser, rs)
	}
	
	class StaffInformationQuery(ds: DataSource, member: MembershipInformation, ssoUser: User) extends MappingSqlQuery[ImportSingleStaffCommand](ds, GetStaffInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		compile()		
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStaffCommand(member, ssoUser, rs)
	}
	
	val GetMembershipByUsercodeInformation = """
		select * from cmsowner.uow_current_members where its_usercode in (:usercodes)
		"""
	  
	class MembershipByUsercodeQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByUsercodeInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = membershipToMember(rs)
	}
	
	val GetMembershipByDepartmentInformation = """
		select * from cmsowner.uow_current_members where id_dept = :departmentCode
		"""
	  
	class MembershipByDepartmentQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByDepartmentInformation) {
		declareParameter(new SqlParameter("departmentCode", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = membershipToMember(rs)
	}
	
	private def membershipToMember(rs: ResultSet) =
		MembershipMember(
			universityId 			= rs.getString("university_number"),
			departmentCode			= rs.getString("id_dept"),
			email					= rs.getString("email"),
			targetGroup				= rs.getString("desc_target_group"),
			title					= rs.getString("pref_title"),
			preferredForenames		= rs.getString("pref_forenames"),
			preferredSurname		= rs.getString("pref_surname"),
			position				= rs.getString("desc_position"),
			dateOfBirth				= rs.getDate("dob"),
			usercode				= rs.getString("its_usercode"),
			startDate				= rs.getDate("dt_start"),
			endDate					= rs.getDate("dt_end"),
			modified				= rs.getDate("dt_modified"),
			phoneNumber				= rs.getString("tel_business"),
			gender					= Gender.fromCode(rs.getString("gender")),
			alternativeEmailAddress	= rs.getString("external_email"),
			userType				= MemberUserType.fromTargetGroup(rs.getString("desc_target_group"))
		)
	
	private implicit def sqlDateToLocalDate(date: java.sql.Date): LocalDate =
		Option(date) map { new LocalDate(_) } orNull
	  
}

case class MembershipMember(
	val universityId: String = null,
	val departmentCode: String = null,
	val email: String = null,
	val targetGroup: String = null,
	val title: String = null,
	val preferredForenames: String = null,
	val preferredSurname: String = null,
	val position: String = null,
	val dateOfBirth: LocalDate = null,
	val usercode: String = null,
	val startDate: LocalDate = null,
	val endDate: LocalDate = null,
	val modified: LocalDate = null,
	val phoneNumber: String = null,
	val gender: Gender = null,
	val alternativeEmailAddress: String = null,
	val userType: MemberUserType
)