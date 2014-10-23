package uk.ac.warwick.tabula.scheduling.services

import java.sql.{ResultSet, Types}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.joda.time.{DateTime, LocalDate}
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service

import javax.sql.DataSource
import uk.ac.warwick.membership.{MembershipInterfaceException, MembershipInterfaceWrapper}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{DegreeType, Department, Gender, Member, MemberUserType}
import uk.ac.warwick.tabula.data.model.MemberUserType.{Emeritus, Other, Staff, Student}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.sandbox.{MapResultSet, SandboxData}
import uk.ac.warwick.tabula.scheduling.commands.imports._
import uk.ac.warwick.tabula.scheduling.helpers.{ImportCommandFactory}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.Features

case class MembershipInformation(val member: MembershipMember, val photo: () => Option[Array[Byte]])

trait ProfileImporter {
	import ProfileImporter._

	var features = Wire[Features]

	def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[UniversityId, User], importCommandFactory: ImportCommandFactory)
		: Seq[ImportMemberCommand]
	def membershipInfoByDepartment(department: Department): Seq[MembershipInformation]
	def membershipInfoForIndividual(member: Member): Option[MembershipInformation]
}

@Profile(Array("dev", "test", "production"))
@Service
class ProfileImporterImpl extends ProfileImporter with Logging with SitsAcademicYearAware {
	import ProfileImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	var membership = Wire[DataSource]("membershipDataSource")
	var membershipInterface = Wire.auto[MembershipInterfaceWrapper]

	lazy val membershipByDepartmentQuery = new MembershipByDepartmentQuery(membership)
	lazy val membershipByUniversityIdQuery = new MembershipByUniversityIdQuery(membership)

	def studentInformationQuery(member: MembershipInformation, ssoUser: User, importCommandFactory: ImportCommandFactory) = {

		val sceYearClause =
			if (features.includePastYears) ""
			else "and sce.sce_ayrc in (:year)"

		new StudentInformationQuery(sits, member, ssoUser, importCommandFactory, sceYearClause)
	}

	def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[UniversityId, User], importCommandFactory: ImportCommandFactory)
		: Seq[ImportMemberCommand] = {
		// TODO we could probably chunk this into 20 or 30 users at a time for the query, or even split by category and query all at once

		val sitsCurrentAcademicYear = getCurrentSitsAcademicYearString

		memberInfo.groupBy(_.member.userType).flatMap { case (userType, members) =>
			userType match {
				case Staff | Emeritus => members.map { info =>
					val ssoUser = users(info.member.universityId)
					new ImportStaffMemberCommand(info, ssoUser)
				}
				case Student | Other => members.par.flatMap { info =>
					val universityId = info.member.universityId
					val ssoUser = users(universityId)

					studentInformationQuery(info, ssoUser, importCommandFactory).executeByNamedParam(
						if (features.includePastYears)
							Map("universityId" -> universityId)
						else
							Map("year" -> sitsCurrentAcademicYear, "universityId" -> universityId)

					).toSeq
				}.seq
				case _ => Seq()
			}
		}.toSeq
	}

	def photoFor(universityId: String): () => Option[Array[Byte]] = {
		def photo() = try {
			logger.info(s"Fetching photo for $universityId")
			Option(membershipInterface.getPhotoById(universityId))
		} catch {
			case e: MembershipInterfaceException => {
				logger.info(s"MembershipInterfaceException fetching photo for ${universityId}: ${e.getMessage}")
				None
			}
		}

		photo
	}

	def membershipInfoByDepartment(department: Department): Seq[MembershipInformation] =
		membershipByDepartmentQuery.executeByNamedParam(Map("departmentCode" -> department.code.toUpperCase)).toSeq map { member =>
			MembershipInformation(member, photoFor(member.universityId))
		}

	def membershipInfoForIndividual(member: Member): Option[MembershipInformation] = {
		membershipByUniversityIdQuery.executeByNamedParam(Map("universityIds" -> member.universityId)).asScala.toList match {
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
	def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[String, User], importCommandFactory: ImportCommandFactory): Seq[ImportMemberCommand] =
		memberInfo map { info => info.member.userType match {
			case Student => studentMemberDetails(importCommandFactory)(info)
			case _ => staffMemberDetails(info)
		}}

	def studentMemberDetails(importCommandFactory: ImportCommandFactory)(mac: MembershipInformation) = {
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
			"course_code" -> "%c%s-%s".format(route.courseType.courseCodeChar, member.departmentCode.toUpperCase, route.code.toUpperCase),
			"course_year_length" -> "3",
			"spr_code" -> "%s/1".format(member.universityId),
			"route_code" -> route.code.toUpperCase,
			"department_code" -> member.departmentCode.toUpperCase,
			"award_code" -> (if (route.degreeType == DegreeType.Undergraduate) "BA" else "MA"),
			"spr_status_code" -> "C",
			"scj_status_code" -> "C",
			"level_code" -> ((member.universityId.toLong % 3) + 1).toString,
			"spr_tutor1" -> null,
			"scj_code" -> "%s/1".format(member.universityId),
			"begin_date" -> member.startDate.toDateTimeAtStartOfDay(),
			"end_date" -> member.endDate.toDateTimeAtStartOfDay(),
			"expected_end_date" -> member.endDate.toDateTimeAtStartOfDay(),
			"most_signif_indicator" -> "Y",
			"funding_source" -> null,
			"enrolment_status_code" -> "C",
			"year_of_study" -> ((member.universityId.toLong % 3) + 1).toInt,
			"mode_of_attendance_code" -> (if (member.universityId.toLong % 5 == 0) "P" else "F"),
			"sce_academic_year" -> AcademicYear.guessSITSAcademicYearByDate(DateTime.now).toString,
			"sce_sequence_number" -> 1,
			"enrolment_department_code" -> member.departmentCode.toUpperCase,
			"mod_reg_status" -> "CON",
			"disability" -> "A"
		))
		ImportStudentRowCommand(
			mac,
			ssoUser,
			rs,
			importCommandFactory
		)
	}

	def staffMemberDetails(mac: MembershipInformation) = {
		val member = mac.member
		val ssoUser = new User(member.usercode)
		ssoUser.setFoundUser(true)
		ssoUser.setVerified(true)
		ssoUser.setDepartment(SandboxData.Departments(member.departmentCode).name)
		ssoUser.setDepartmentCode(member.departmentCode)
		ssoUser.setEmail(member.email)
		ssoUser.setFirstName(member.preferredForenames)
		ssoUser.setLastName(member.preferredSurname)
		ssoUser.setStaff(true)
		ssoUser.setWarwickId(member.universityId)

		new ImportStaffMemberCommand(mac, ssoUser)
	}

	def membershipInfoByDepartment(department: Department): Seq[MembershipInformation] = {
		val dept = SandboxData.Departments(department.code)

		studentsForDepartment(dept) ++ staffForDepartment(dept)
	}

	def staffForDepartment(department: SandboxData.Department) =
		(department.staffStartId to department.staffEndId).map { uniId =>
			val gender = if (uniId % 2 == 0) Gender.Male else Gender.Female
			val name = SandboxData.randomName(uniId, gender)
			val title = "Professor"
			val userType = MemberUserType.Staff
			val groupName = "Academic staff"

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
					DateTime.now.minusYears(40).toLocalDate().withDayOfYear((uniId % 364) + 1),
					department.code + "s" + uniId.toString.takeRight(3),
					DateTime.now.minusYears(10).toLocalDate,
					null,
					DateTime.now,
					null,
					gender,
					null,
					userType
				), () => None
			)
		}.toSeq

	def studentsForDepartment(department: SandboxData.Department) =
		department.routes.values.flatMap { route =>
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

	def membershipInfoForIndividual(member: Member): Option[MembershipInformation] =
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

object ProfileImporter extends Logging {
	var features = Wire[Features]

	type UniversityId = String

	val sitsSchema: String = Wire.property("${schema.sits}")

	val sceYearClause =
		if (features.includePastYears) {
			""
		}
		else {
			"and sce.sce_ayrc in (:year)"
		}

	def GetStudentInformation(sceYearClause: String) = f"""
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
			stu.stu_dsbc as disability,

			nat.nat_name as nationality,

			crs.crs_code as course_code,
			crs.crs_ylen as course_year_length,

			spr.spr_code as spr_code,
			spr.rou_code as route_code,
			spr.spr_dptc as department_code,
			spr.awd_code as award_code,
			spr.sts_code as spr_status_code,
			spr.spr_levc as level_code,
			prs.prs_udf1 as spr_tutor1,

			scj.scj_code as scj_code,
			scj.scj_begd as begin_date,
			scj.scj_endd as end_date,
			scj.scj_eend as expected_end_date,
			scj.scj_udfa as most_signif_indicator,
			scj.scj_stac as scj_status_code,
	 		scj.scj_prsc as scj_tutor1,

			sce.sce_sfcc as funding_source,
			sce.sce_stac as enrolment_status_code,
			sce.sce_blok as year_of_study,
			sce.sce_moac as mode_of_attendance_code,
			sce.sce_ayrc as sce_academic_year,
			sce.sce_seq2 as sce_sequence_number,
			sce.sce_dptc as enrolment_department_code,

			ssn.ssn_mrgs as mod_reg_status

		from $sitsSchema.ins_stu stu

			join $sitsSchema.ins_spr spr
				on stu.stu_code = spr_stuc

			join $sitsSchema.srs_scj scj
				on spr.spr_code = scj.scj_sprc

			join $sitsSchema.srs_sce sce
				on scj.scj_code = sce.sce_scjc
				$sceYearClause
				and sce.sce_seq2 =
					(
						select max(sce2.sce_seq2)
							from $sitsSchema.srs_sce sce2
								where sce.sce_scjc = sce2.sce_scjc
								and sce2.sce_ayrc = sce.sce_ayrc
					)

			left outer join $sitsSchema.srs_crs crs
				on sce.sce_crsc = crs.crs_code

			left outer join $sitsSchema.srs_nat nat
				on stu.stu_natc = nat.nat_code

			left outer join $sitsSchema.srs_sta sts
				on spr.sts_code = sts.sta_code

			left outer join $sitsSchema.cam_ssn ssn
				on spr.spr_code = ssn.ssn_sprc
				and sce.sce_ayrc = ssn.ssn_ayrc

			left outer join $sitsSchema.ins_prs prs
				on spr.prs_code = prs.prs_code

		where stu.stu_code = :universityId
		order by stu.stu_code
		"""

	class StudentInformationQuery(ds: DataSource,
																member: MembershipInformation,
																ssoUser: User,
																importCommandFactory: ImportCommandFactory,
																sceYearClause: String)
		extends MappingSqlQuery[ImportStudentRowCommandInternal](ds, GetStudentInformation(sceYearClause)) {

		var features = Wire.auto[Features]

		declareParameter(new SqlParameter("universityId", Types.VARCHAR))

		if (!features.includePastYears)
			declareParameter(new SqlParameter("year", Types.VARCHAR))

		compile()

		override def mapRow(rs: ResultSet, rowNumber: Int)
		= ImportStudentRowCommand(
			member,
			ssoUser,
			rs,
			importCommandFactory
		)
	}

	val GetMembershipByUniversityIdInformation = """
		select * from cmsowner.uow_current_members where university_number in (:universityIds) and its_usercode is not null
		"""

	class MembershipByUniversityIdQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByUniversityIdInformation) {
		declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
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
