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
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.membership.MembershipInterfaceException
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleStudentRowCommand


@Service
class StudentImporter extends Logging {
	import StudentImporter._

	var sits = Wire[DataSource]("sitsDataSource")
	var membership = Wire[DataSource]("membershipDataSource")
	var membershipInterface = Wire.auto[MembershipInterfaceWrapper]

	lazy val currentAcademicYear = new GetCurrentAcademicYearQuery(sits).execute().head

	def allStudentInformationQuery() = new AllStudentInfoQuery(sits)

	def studentInformationQuery(uniId: String) = new StudentInfoQuery(sits, uniId)

	def getStudentDetails(universityId: String): Seq[ImportSingleStudentRowCommand] = {
		// TODO we could probably chunk this into 20 or 30 users at a time for the query, or even split by category and query all at once

		studentInformationQuery(universityId).executeByNamedParam(Map("year" -> currentAcademicYear, "id" -> universityId)).toSeq
	}

	def getAllStudentDetails(): Seq[ImportSingleStudentRowCommand] = {
		allStudentInformationQuery.executeByNamedParam(Map("year" -> currentAcademicYear)).toSeq
	}

	def photoFor(universityId: String): () => Option[Array[Byte]] = {
		def photo() = try {
			Option(membershipInterface.getPhotoById(universityId))
		} catch {
			case e: MembershipInterfaceException => None
		}

		photo
	}
}

object StudentImporter {

	val GetAllStudentInformation = """
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

			spr.spr_code as spr_code,
			spr.rou_code as route_code,
			spr.spr_dptc as study_department,
			spr.awd_code as award_code,
			spr.sts_code as spr_status_code,
			--spr.spr_levc as level_code,
			spr.prs_code as spr_tutor1,
			--spr.spr_prs2 as spr_tutor2,

			scj.scj_code as scj_code,
			scj.scj_begd as begin_date,
			scj.scj_endd as end_date,
			scj.scj_eend as expected_end_date,
			--scj.scj_prsc as scj_tutor1,
			--scj.scj_prs2 as scj_tutor2,

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

			where spr.sts_code not like 'P%' and scj.scj_stac not like 'P%'
		"""

	val GetSingleStudentInformation = GetAllStudentInformation + "and stu.stu_code = (:uniId)"

	class StudentInfoQuery(ds: DataSource, uniId: String)
		extends MappingSqlQuery[ImportSingleStudentRowCommand](ds, GetSingleStudentInformation) {
			declareParameter(new SqlParameter("uniId", Types.VARCHAR))
			declareParameter(new SqlParameter("year", Types.VARCHAR))
			compile()
			override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStudentRowCommand(rs)
	}

	class AllStudentInfoQuery(ds: DataSource)
		extends MappingSqlQuery[ImportSingleStudentRowCommand](ds, GetAllStudentInformation) {
			declareParameter(new SqlParameter("year", Types.VARCHAR))
			compile()
			override def mapRow(rs: ResultSet, rowNumber: Int) = new ImportSingleStudentRowCommand(rs)
	}

	val GetCurrentAcademicYear = """
		select UWTABS.GET_AYR() ayr from dual
		"""

	class GetCurrentAcademicYearQuery(ds: DataSource) extends MappingSqlQuery[String](ds, GetCurrentAcademicYear) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = rs.getString("ayr")
	}

	private def sqlDateToLocalDatex(date: java.sql.Date): LocalDate =
		(Option(date) map { new LocalDate(_) }).orNull
}
