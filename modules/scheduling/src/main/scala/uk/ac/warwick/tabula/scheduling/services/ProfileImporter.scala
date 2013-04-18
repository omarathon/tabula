package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import java.sql.Types
import scala.collection.JavaConversions._
import org.joda.time.LocalDate
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.membership.MembershipInterfaceWrapper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.MemberUserType.Emeritus
import uk.ac.warwick.tabula.data.model.MemberUserType.Staff
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleMemberCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStaffCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentCommand
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.membership.MembershipInterfaceException

case class MembershipInformation(val member: MembershipMember, val photo: Option[Array[Byte]])

@Service
class ProfileImporter extends Logging {
	import ProfileImporter._

	var sits = Wire[DataSource]("sitsDataSource")
	var membership = Wire[DataSource]("membershipDataSource")
	var membershipInterface = Wire[MembershipInterfaceWrapper]

	lazy val currentAcademicYear = new GetCurrentAcademicYearQuery(sits).execute().head

	lazy val membershipByDepartmentQuery = new MembershipByDepartmentQuery(membership)
	lazy val membershipByUsercodeQuery = new MembershipByUsercodeQuery(membership)

	def studentInformationQuery(member: MembershipInformation, ssoUser: User) = new StudentInformationQuery(sits, member, ssoUser)
	def staffInformationQuery(member: MembershipInformation, ssoUser: User) = new StaffInformationQuery(sits, member, ssoUser)

	def getMemberDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportSingleMemberCommand] = {
		// TODO we could probably chunk this into 20 or 30 users at a time for the query, or even split by category and query all at once

		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(usercode)

			mac.member.userType match {
				case Student 		   => studentInformationQuery(mac, ssoUser).executeByNamedParam(
											Map("year" -> currentAcademicYear, "usercodes" -> usercode)
										  ).toSeq
				case Staff | Emeritus  => staffInformationQuery(mac, ssoUser).executeByNamedParam(Map("usercodes" -> usercode)).toSeq
				case _ => Seq()
			}
		}
	}

	def userIdsAndCategories(department: Department): Seq[MembershipInformation] =
		membershipByDepartmentQuery.executeByNamedParam(Map("departmentCode" -> department.code.toUpperCase)).toSeq map { member =>
			val photo = try {
				Option(membershipInterface.getPhotoById(member.universityId))
			} catch {
				case e: MembershipInterfaceException => None
			}
			
			MembershipInformation(member, photo)
		}

	def userIdAndCategory(member: Member) =
		MembershipInformation(
			membershipByUsercodeQuery.executeByNamedParam(Map("usercodes" -> member.userId)).head,
			Option(membershipInterface.getPhotoById(member.universityId))
		)
}

object ProfileImporter {

	val GetStudentInformation = """
		select
			stu.stu_code as university_id,
			stu.stu_titl as title,
			stu.stu_fusd as preferred_forename,
			trim(stu.stu_fnm1 || ' ' || stu.stu_fnm2 || ' ' || stu.stu_fnm3) as forenames,
			stu.stu_surn as family_name,
			stu.stu_gend as gender,
			stu.stu_caem as email_address,
			stu.stu_udf3 as user_code,
			stu.stu_dob as date_of_birth,
			case when stu.stu_endd < sysdate then 'Inactive' else 'Active' end as in_use_flag,
			stu.stu_endd as date_of_inactivation,
			stu.stu_haem as alternative_email_address,
			stu.stu_cat3 as mobile_number,

			nat.nat_name as nationality,

			crs.crs_code as sits_course_code,
			crs.crs_ylen as course_year_length,
			decode(crs.crs_schc,'UW PG', 'postgraduate', 'undergraduate') as ug_pg,

			spr.spr_code as spr_code,
			spr.rou_code as route_code,
			spr.spr_dptc as study_department,
			spr.awd_code as award_code,
			spr.sts_code as spr_status_code,
			--spr.spr_levc as level_code,

			scj.scj_begd as begin_date,
			scj.scj_endd as end_date,
			scj.scj_eend as expected_end_date,

			sce.sce_sfcc as funding_source,
			sce.sce_stac as enrolment_status_code,
			sce.sce_blok as year_of_study,
			sce.sce_moac as mode_of_attendance_code

		from intuit.ins_stu stu

			join intuit.ins_spr spr
				on stu.stu_code = spr_stuc

			join intuit.srs_scj scj
				on spr.spr_code = scj.scj_sprc
				and scj.scj_udfa = 'Y'

			join intuit.srs_sce sce
				on scj.scj_code = sce.sce_scjc
				and sce.sce_ayrc in (:year)
				and sce.sce_seq2 =
					(
						select max(sce2.sce_seq2)
							from srs_sce sce2
								where sce.sce_scjc = sce2.sce_scjc
								and sce2.sce_ayrc = sce.sce_ayrc
					)

			left outer join intuit.srs_crs crs
				on sce.sce_crsc = crs.crs_code

			left outer join intuit.srs_nat nat
				on stu.stu_natc = nat.nat_code

			left outer join intuit.srs_sta sts
				on spr.sts_code = sts.sta_code

			where stu.stu_udf3 in (:usercodes)
		"""

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
		from intuit.ins_prs prs
			where prs.prs_exid in (:usercodes)
		"""

	class StudentInformationQuery(ds: DataSource, member: MembershipInformation, ssoUser: User) extends MappingSqlQuery[ImportSingleStudentCommand](ds, GetStudentInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		declareParameter(new SqlParameter("year", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStudentCommand(member, ssoUser, rs)
	}

	class StaffInformationQuery(ds: DataSource, member: MembershipInformation, ssoUser: User) extends MappingSqlQuery[ImportSingleStaffCommand](ds, GetStaffInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStaffCommand(member, ssoUser, rs)
	}

	val GetCurrentAcademicYear = """
		select UWTABS.GET_AYR() ayr from dual
		"""

	class GetCurrentAcademicYearQuery(ds: DataSource) extends MappingSqlQuery[String](ds, GetCurrentAcademicYear) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = rs.getString("ayr")
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
		select * from cmsowner.uow_current_members where id_dept = :departmentCode and its_usercode is not null
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
			dateOfBirth				= sqlDateToLocalDate(rs.getDate("dob")),
			usercode				= rs.getString("its_usercode"),
			startDate				= sqlDateToLocalDate(rs.getDate("dt_start")),
			endDate					= sqlDateToLocalDate(rs.getDate("dt_end")),
			modified				= sqlDateToLocalDate(rs.getDate("dt_modified")),
			phoneNumber				= rs.getString("tel_business"),
			gender					= Gender.fromCode(rs.getString("gender")),
			alternativeEmailAddress	= rs.getString("external_email"),
			userType				= MemberUserType.fromTargetGroup(rs.getString("desc_target_group"))
		)

	private def sqlDateToLocalDate(date: java.sql.Date): LocalDate =
		(Option(date) map { new LocalDate(_) }).orNull

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