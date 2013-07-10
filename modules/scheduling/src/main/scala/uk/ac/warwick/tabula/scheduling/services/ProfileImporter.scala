package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import java.sql.Types
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
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
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentRowCommand
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.membership.MembershipInterfaceException
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.scheduling.sandbox.SandboxData
import uk.ac.warwick.tabula.data.model.DegreeType
import uk.ac.warwick.tabula.scheduling.sandbox.MapResultSet

case class MembershipInformation(val member: MembershipMember, val photo: () => Option[Array[Byte]])

trait ProfileImporter {
	def getMemberDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportSingleMemberCommand]
	def userIdsAndCategories(department: Department): Seq[MembershipInformation]
	def userIdAndCategory(member: Member): Option[MembershipInformation]
}

@Profile(Array("dev", "test", "production")) @Service
class ProfileImporterImpl extends ProfileImporter with Logging {
	import ProfileImporter._

	var sits = Wire[DataSource]("sitsDataSource")
	var membership = Wire[DataSource]("membershipDataSource")
	var membershipInterface = Wire.auto[MembershipInterfaceWrapper]

	lazy val currentAcademicYear = new GetCurrentAcademicYearQuery(sits).execute().head

	lazy val membershipByDepartmentQuery = new MembershipByDepartmentQuery(membership)
	lazy val membershipByUsercodeQuery = new MembershipByUsercodeQuery(membership)

	def studentInformationQuery(member: MembershipInformation, ssoUser: User) = {
		val ret = new StudentInformationQuery(sits, member, ssoUser)
		ret
	}
	def staffInformationQuery(member: MembershipInformation, ssoUser: User) = new StaffInformationQuery(sits, member, ssoUser)

	def getMemberDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportSingleMemberCommand] = {
		// TODO we could probably chunk this into 20 or 30 users at a time for the query, or even split by category and query all at once

		membersAndCategories flatMap { mac =>
			val usercode = mac.member.usercode
			val ssoUser = users(usercode)

			mac.member.userType match {
				case Student 		   => {
					val cmds = studentInformationQuery(mac, ssoUser).executeByNamedParam(
											Map("year" -> currentAcademicYear, "usercodes" -> usercode)
										  ).toSeq
					cmds
					}
				case Staff | Emeritus  => staffInformationQuery(mac, ssoUser).executeByNamedParam(Map("usercodes" -> usercode)).toSeq
				case _ => Seq()
			}
		}
	}

	def photoFor(universityId: String): () => Option[Array[Byte]] = {
		def photo() = try {
			logger.info(s"Fetching photo for $universityId")
			Option(membershipInterface.getPhotoById(universityId))
		} catch {
			case e: MembershipInterfaceException => None
		}

		photo
	}

	def userIdsAndCategories(department: Department): Seq[MembershipInformation] =
		membershipByDepartmentQuery.executeByNamedParam(Map("departmentCode" -> department.code.toUpperCase)).toSeq map { member =>
			MembershipInformation(member, photoFor(member.universityId))
		}

	def userIdAndCategory(member: Member): Option[MembershipInformation] = {
		membershipByUsercodeQuery.executeByNamedParam(Map("usercodes" -> member.userId)).asScala.toList match {
			case Nil => None
			case mem: List[MembershipMember] => Some (
					MembershipInformation(
						mem.head,
						photoFor(member.universityId)
					)
				)
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxProfileImporter extends ProfileImporter {
	def getMemberDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportSingleMemberCommand] =
		membersAndCategories map { mac =>
			val member = mac.member
			val ssoUser = new User(member.usercode)
			ssoUser.setFoundUser(true)
			ssoUser.setVerified(true)
			ssoUser.setDepartment(SandboxData.Departments(member.departmentCode).name)
			ssoUser.setDepartmentCode(member.departmentCode)
			ssoUser.setEmail(member.email)
			ssoUser.setFirstName(member.preferredForenames)
			ssoUser.setLastName(member.preferredSurname)
			ssoUser.setStudent(true)
			ssoUser.setWarwickId(member.universityId)
			
			val route = SandboxData.route(member.universityId.toLong)
			
			val rs = new MapResultSet(Map(
				"university_id" -> member.universityId,
				"title" -> member.title,
				"preferred_forename" -> member.preferredForenames,
				"forenames" -> member.preferredForenames,
				"family_name" -> member.preferredSurname,
				"gender" -> member.gender.dbValue,
				"email_address" -> member.email,
				"user_code" -> member.usercode,
				"date_of_birth" -> member.dateOfBirth.toDateTimeAtStartOfDay(),
				"in_use_flag" -> "Active",
				"date_of_inactivation" -> member.endDate.toDateTimeAtStartOfDay(),
				"alternative_email_address" -> null,
				"mobile_number" -> null,
				"nationality" -> "British (ex. Channel Islands & Isle of Man)",
				"sits_course_code" -> "%s-%s".format(member.departmentCode.toUpperCase, route.code.toUpperCase),
				"course_year_length" -> "3",
				"spr_code" -> "%s/1".format(member.universityId),
				"route_code" -> route.code.toUpperCase,
				"study_department" -> member.departmentCode.toUpperCase,
				"award_code" -> (if (route.degreeType == DegreeType.Undergraduate) "BA" else "MA"),
				"spr_status_code" -> "C",
				"spr_tutor1" -> null,
				"scj_code" -> "%s/1".format(member.universityId),
				"begin_date" -> member.startDate.toDateTimeAtStartOfDay(),
				"end_date" -> member.endDate.toDateTimeAtStartOfDay(),
				"expected_end_date" -> member.endDate.toDateTimeAtStartOfDay(),
				"funding_source" -> null,
				"enrolment_status_code" -> "F",
				"year_of_study" -> ((member.universityId.toLong % 3) + 1).toInt,
				"mode_of_attendance_code" -> (if (member.universityId.toLong % 5 == 0) "P" else "F")
			))
			new ImportSingleStudentRowCommand(mac, ssoUser, rs)
		}
		
	def userIdsAndCategories(department: Department): Seq[MembershipInformation] = 
		SandboxData.Departments(department.code).routes.values.flatMap { route =>
			(route.studentsStartId to route.studentsEndId).map { uniId =>
				val gender = if (uniId % 2 == 0) Gender.Male else Gender.Female
				val name = SandboxData.randomName(uniId, gender)
				val title = gender match {
					case Gender.Male => "Mr"
					case _ => "Miss"
				}
				// Every fifth student is part time
				val isPartTime = uniId % 5 == 0
				
				val userType = MemberUserType.Student
				val groupName = route.degreeType match {
					case DegreeType.Undergraduate => if (isPartTime) "Undergraduate - part-time" else "Undergraduate - full-time"
					case _ => 
						if (route.isResearch)
							if (isPartTime) "Postgraduate (research) PT" else "Postgraduate (research) FT"
						else
							if (isPartTime) "Postgraduate (taught) PT" else "Postgraduate (taught) FT"
				}
				
				MembershipInformation(
					MembershipMember(
						uniId.toString,
						department.code,
						"%s.%s@tabula-sandbox.warwick.ac.uk".format(name.givenName.substring(0, 1), name.familyName),
						groupName,
						title,
						name.givenName,
						name.familyName,
						groupName,
						DateTime.now.minusYears(19).toLocalDate().withDayOfYear((uniId % 364) + 1),
						department.code + uniId.toString.takeRight(4),
						DateTime.now.minusYears(1).toLocalDate,
						DateTime.now.plusYears(2).toLocalDate,
						DateTime.now,
						null,
						gender,
						null,
						userType
					), () => None
				)
			}
		}.toSeq
		
	def userIdAndCategory(member: Member): Option[MembershipInformation] = 
		Some(MembershipInformation(
			MembershipMember(
				member.universityId,
				member.homeDepartment.code,
				member.email,
				member.groupName,
				member.title,
				member.firstName,
				member.lastName,
				member.jobTitle,
				member.dateOfBirth,
				member.userId,
				DateTime.now.minusYears(1).toLocalDate,
				DateTime.now.plusYears(2).toLocalDate,
				member.lastUpdatedDate,
				member.phoneNumber,
				member.gender,
				member.homeEmail,
				member.userType
			), () => None
		))
		
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

			crs.crs_code as course_code,
			crs.crs_ylen as course_year_length,

			spr.spr_code as spr_code,
			spr.rou_code as route_code,
			spr.spr_dptc as department_code,
			spr.awd_code as award_code,
			spr.sts_code as spr_status_code,
			spr.spr_levc as level_code,
			spr.prs_code as spr_tutor1,
			--spr.spr_prs2 as spr_tutor2,

			scj.scj_code as scj_code,
			scj.scj_begd as begin_date,
			scj.scj_endd as end_date,
			scj.scj_eend as expected_end_date,
			scj.scj_udfa as most_signif_indicator,
			--scj.scj_prsc as scj_tutor1,
			--scj.scj_prs2 as scj_tutor2,

			sce.sce_sfcc as funding_source,
			sce.sce_stac as enrolment_status_code,
			sce.sce_blok as year_of_study,
			sce.sce_moac as mode_of_attendance_code,
			sce.sce_ayrc as sce_academic_year,
			sce.sce_seq2 as sce_sequence_number

		from intuit.ins_stu stu

			join intuit.ins_spr spr
				on stu.stu_code = spr_stuc

			join intuit.srs_scj scj
				on spr.spr_code = scj.scj_sprc

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

	class StudentInformationQuery(ds: DataSource, member: MembershipInformation, ssoUser: User)
		extends MappingSqlQuery[ImportSingleStudentRowCommand](ds, GetStudentInformation) {
		declareParameter(new SqlParameter("usercodes", Types.VARCHAR))
		declareParameter(new SqlParameter("year", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStudentRowCommand(member, ssoUser, rs)
	}

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

	class StaffInformationQuery(ds: DataSource, member: MembershipInformation, ssoUser: User)
		extends MappingSqlQuery[ImportSingleStaffCommand](ds, GetStaffInformation) {
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
			modified				= sqlDateToDateTime(rs.getDate("dt_modified")),
			phoneNumber				= rs.getString("tel_business"),
			gender					= Gender.fromCode(rs.getString("gender")),
			alternativeEmailAddress	= rs.getString("external_email"),
			userType				= MemberUserType.fromTargetGroup(rs.getString("desc_target_group"))
		)

	private def sqlDateToLocalDate(date: java.sql.Date): LocalDate =
		(Option(date) map { new LocalDate(_) }).orNull

	private def sqlDateToDateTime(date: java.sql.Date): DateTime =
		(Option(date) map { new DateTime(_) }).orNull

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
	val modified: DateTime = null,
	val phoneNumber: String = null,
	val gender: Gender = null,
	val alternativeEmailAddress: String = null,
	val userType: MemberUserType
)
