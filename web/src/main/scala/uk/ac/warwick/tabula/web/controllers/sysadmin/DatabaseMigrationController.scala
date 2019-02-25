package uk.ac.warwick.tabula.web.controllers.sysadmin

import java.io.PrintWriter
import java.sql._
import java.time.OffsetDateTime

import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PostMapping, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.Mav

import scala.collection.mutable

object DatabaseMigrationController {
	case class Migration(tableName: String, migrations: Seq[MigrationColumn[_]], restriction: Option[String] = None)
	class MigrationColumn[A](val columnName: String, val read: (ResultSet, Int) => A, val write: (PreparedStatement, Int, A) => Unit, val jdbcType: JDBCType) {
		def process(index: Int, rs: ResultSet, ps: PreparedStatement): Unit = {
			val value = read(rs, index)
			if (rs.wasNull()) {
				ps.setNull(index, jdbcType.getVendorTypeNumber)
			} else {
				write(ps, index, value)
			}
		}
	}

	case class StringColumn(col: String) extends MigrationColumn[String](col, _.getString(_), _.setString(_, _), JDBCType.VARCHAR)
	case class IntColumn(col: String) extends MigrationColumn[JInteger](col, _.getInt(_), _.setInt(_, _), JDBCType.INTEGER)
	case class BigDecimalColumn(col: String) extends MigrationColumn[JBigDecimal](col, _.getBigDecimal(_), _.setBigDecimal(_, _), JDBCType.BIGINT)
	case class TimestampColumn(col: String) extends MigrationColumn[Timestamp](col, _.getTimestamp(_), _.setTimestamp(_, _), JDBCType.TIMESTAMP)
	case class BooleanColumn(col: String) extends MigrationColumn[JBoolean](col, _.getInt(_) == 1, _.setBoolean(_, _), JDBCType.BOOLEAN)
	case class DateColumn(col: String) extends MigrationColumn[Date](col, _.getDate(_), _.setDate(_, _), JDBCType.DATE)

	def generateMappings(connection: Connection, writer: PrintWriter): Unit = {
		val tables = connection.getMetaData.getTables(null, null, "%", scala.Array("TABLE"))
		val tableNames: mutable.ListBuffer[String] = mutable.ListBuffer()
		while (tables.next()) {
			tableNames += tables.getString("TABLE_NAME")
		}
		tables.close()

		tableNames.filterNot(_ == "objectcache").filterNot(_ == "flyway_schema_history").filterNot(_.startsWith("qrtz_")).foreach { table =>
			writer.println(s"""Migration("$table", Seq(""")

			val columns = connection.getMetaData.getColumns(null, null, table, "%")
			while (columns.next()) {
				val columnName = columns.getString("COLUMN_NAME")
				val typeName = columns.getString("TYPE_NAME")
				val size = columns.getInt("COLUMN_SIZE")
				val digits = columns.getInt("DECIMAL_DIGITS")

				val caseClass = typeName match {
					case "varchar" | "text" | "bpchar" => "StringColumn"
					case "numeric" if digits > 0 => "BigDecimalColumn"
					case "float4" => "BigDecimalColumn"
					case "numeric" | "int2" | "int4" => "IntColumn"
					case "timestamp" => "TimestampColumn"
					case "bool" => "BooleanColumn"
					case "date" => "DateColumn"
					case _ => throw new IllegalArgumentException(s"$table.$columnName $typeName($size, $digits)")
				}

				writer.println(s"""  $caseClass("$columnName"),""")
			}
			columns.close()

			writer.println(")),")
			writer.println()
		}
	}

	val mappings: Seq[Migration] = Seq(
		Migration("accreditedpriorlearning", Seq(
			StringColumn("id"),
			StringColumn("scjcode"),
			StringColumn("awardcode"),
			IntColumn("sequencenumber"),
			IntColumn("academicyear"),
			BigDecimalColumn("cats"),
			StringColumn("levelcode"),
			StringColumn("reason"),
			TimestampColumn("lastupdateddate"),
			IntColumn("hib_version"),
		)),

		Migration("address", Seq(
			StringColumn("id"),
			StringColumn("line1"),
			StringColumn("line2"),
			StringColumn("line3"),
			StringColumn("line4"),
			StringColumn("line5"),
			StringColumn("postcode"),
			StringColumn("telephone"),
		)),

		Migration("assessmentgroup", Seq(
			StringColumn("id"),
			StringColumn("assignment_id"),
			StringColumn("upstream_id"),
			StringColumn("occurrence"),
			StringColumn("group_set_id"),
			StringColumn("exam_id"),
		)),

		// Department must come before MarkingWorkflow
		Migration("department", Seq(
			StringColumn("id"),
			StringColumn("code"),
			StringColumn("name"),
			StringColumn("settings"),
			StringColumn("parent_id"),
			StringColumn("filterrulename"),
			StringColumn("shortname"),
		)),

		// MarkingWorkflow must come before Assignment (and StageMarkers)
		Migration("markingworkflow", Seq(
			StringColumn("id"),
			StringColumn("workflowtype"),
			StringColumn("name"),
			StringColumn("department_id"),
			BooleanColumn("is_reusable"),
			IntColumn("academicyear"),
			StringColumn("settings"),
		)),

		Migration("assignment", Seq(
			StringColumn("id"),
			IntColumn("academicyear"),
			IntColumn("attachmentlimit"),
			TimestampColumn("closedate"),
			StringColumn("fileextensions"),
			StringColumn("name"),
			TimestampColumn("opendate"),
			StringColumn("module_id"),
			BooleanColumn("collectmarks"),
			BooleanColumn("deleted"),
			BooleanColumn("collectsubmissions"),
			BooleanColumn("restrictsubmissions"),
			BooleanColumn("allowlatesubmissions"),
			BooleanColumn("allowresubmission"),
			BooleanColumn("displayplagiarismnotice"),
			StringColumn("membersgroup_id"),
			BooleanColumn("archived"),
			TimestampColumn("createddate"),
			BooleanColumn("allowextensions"),
			StringColumn("markscheme_id"),
			StringColumn("feedback_template_id"),
			BooleanColumn("openended"),
			BooleanColumn("summative"),
			StringColumn("genericfeedback"),
			BooleanColumn("dissertation"),
			StringColumn("settings"),
			StringColumn("turnitin_id"),
			BooleanColumn("hidden_from_students"),
			BooleanColumn("submittoturnitin"),
			TimestampColumn("lastsubmittedtoturnitin"),
			IntColumn("submittoturnitinretries"),
			TimestampColumn("openendedreminderdate"),
			StringColumn("workflow_category"),
			StringColumn("cm2_workflow_id"),
			BooleanColumn("cm2assignment"),
			StringColumn("anonymous_marking_method"),
		)),

		Migration("attendancemonitoringcheckpoint", Seq(
			StringColumn("id"),
			StringColumn("point_id"),
			StringColumn("student_id"),
			StringColumn("state"),
			TimestampColumn("updated_date"),
			StringColumn("updated_by"),
			BooleanColumn("autocreated"),
		)),

		Migration("attendancemonitoringpoint", Seq(
			StringColumn("id"),
			StringColumn("scheme_id"),
			StringColumn("name"),
			IntColumn("start_week"),
			IntColumn("end_week"),
			TimestampColumn("start_date"),
			TimestampColumn("end_date"),
			StringColumn("point_type"),
			TimestampColumn("created_date"),
			TimestampColumn("updated_date"),
			StringColumn("settings"),
		)),

		Migration("attendancemonitoringscheme", Seq(
			StringColumn("id"),
			StringColumn("name"),
			IntColumn("academicyear"),
			StringColumn("membersgroup_id"),
			StringColumn("member_query"),
			StringColumn("point_style"),
			StringColumn("department_id"),
			TimestampColumn("created_date"),
			TimestampColumn("updated_date"),
		)),

		Migration("attendancemonitoringtotal", Seq(
			StringColumn("id"),
			StringColumn("student_id"),
			IntColumn("unrecorded"),
			IntColumn("authorized"),
			IntColumn("unauthorized"),
			IntColumn("attended"),
			TimestampColumn("updated_date"),
			StringColumn("department_id"),
			IntColumn("academicyear"),
			TimestampColumn("low_level_notified"),
			TimestampColumn("medium_level_notified"),
			TimestampColumn("high_level_notified"),
		)),

		Migration("attendancenote", Seq(
			StringColumn("id"),
			StringColumn("student_id"),
			TimestampColumn("updateddate"),
			StringColumn("updatedby"),
			StringColumn("note"),
			StringColumn("attachment_id"),
			StringColumn("discriminator"),
			StringColumn("parent_id"),
			StringColumn("absence_type"),
		)),

		Migration("attendancetemplate", Seq(
			StringColumn("id"),
			TimestampColumn("createddate"),
			TimestampColumn("updateddate"),
			StringColumn("point_style"),
			StringColumn("templatename"),
			IntColumn("position"),
		)),

		Migration("attendancetemplatepoint", Seq(
			StringColumn("id"),
			StringColumn("scheme_template_id"),
			StringColumn("name"),
			IntColumn("start_week"),
			IntColumn("end_week"),
			TimestampColumn("start_date"),
			TimestampColumn("end_date"),
			TimestampColumn("created_date"),
			TimestampColumn("updated_date"),
		)),

		Migration("award", Seq(
			StringColumn("code"),
			StringColumn("shortname"),
			StringColumn("name"),
			TimestampColumn("lastupdateddate"),
		)),

		Migration("corerequiredmodule", Seq(
			StringColumn("id"),
			StringColumn("routecode"),
			IntColumn("academicyear"),
			IntColumn("yearofstudy"),
			StringColumn("modulecode"),
		)),

		Migration("course", Seq(
			StringColumn("code"),
			StringColumn("shortname"),
			StringColumn("name"),
			StringColumn("title"),
			TimestampColumn("lastupdateddate"),
			StringColumn("department_id"),
			BooleanColumn("inuse"),
		)),

		Migration("courseyearweighting", Seq(
			StringColumn("id"),
			StringColumn("coursecode"),
			IntColumn("academicyear"),
			IntColumn("yearofstudy"),
			BigDecimalColumn("weighting"),
		)),

		Migration("customroledefinition", Seq(
			StringColumn("id"),
			StringColumn("department_id"),
			StringColumn("name"),
			StringColumn("custom_base_role_id"),
			StringColumn("builtinbaseroledefinition"),
			IntColumn("hib_version"),
			BooleanColumn("candelegate"),
			BooleanColumn("replaces_parent"),
		)),

		Migration("departmentsmallgroup", Seq(
			StringColumn("id"),
			StringColumn("set_id"),
			StringColumn("name"),
			StringColumn("studentsgroup_id"),
		)),

		Migration("departmentsmallgroupset", Seq(
			StringColumn("id"),
			StringColumn("department_id"),
			IntColumn("academicyear"),
			StringColumn("name"),
			BooleanColumn("archived"),
			BooleanColumn("deleted"),
			StringColumn("membersgroup_id"),
			StringColumn("member_query"),
		)),

		Migration("disability", Seq(
			StringColumn("code"),
			StringColumn("shortname"),
			StringColumn("sitsdefinition"),
			StringColumn("tabuladefinition"),
			TimestampColumn("lastupdateddate"),
		)),

		Migration("exam", Seq(
			StringColumn("id"),
			StringColumn("name"),
			IntColumn("academicyear"),
			StringColumn("module_id"),
			StringColumn("membersgroup_id"),
			BooleanColumn("deleted"),
			StringColumn("workflow_id"),
			BooleanColumn("released"),
		)),

		Migration("extension", Seq(
			StringColumn("id"),
			TimestampColumn("expirydate"),
			StringColumn("universityid"),
			StringColumn("userid"),
			StringColumn("assignment_id"),
			TimestampColumn("approvedon"),
			StringColumn("approvalcomments"),
			TimestampColumn("requestedexpirydate"),
			TimestampColumn("requestedon"),
			BooleanColumn("disabilityadjustment"),
			StringColumn("state"),
			StringColumn("reason"),
		)),

		Migration("feedback", Seq(
			StringColumn("id"),
			StringColumn("uploaderid"),
			TimestampColumn("uploaded_date"),
			StringColumn("universityid"),
			StringColumn("assignment_id"),
			BooleanColumn("released"),
			IntColumn("rating"),
			BooleanColumn("ratingprompt"),
			BooleanColumn("ratinghelpful"),
			IntColumn("actualmark"),
			StringColumn("actualgrade"),
			IntColumn("agreedmark"),
			StringColumn("agreedgrade"),
			StringColumn("first_marker_feedback"),
			StringColumn("second_marker_feedback"),
			TimestampColumn("released_date"),
			StringColumn("third_marker_feedback"),
			TimestampColumn("updated_date"),
			StringColumn("discriminator"),
			StringColumn("exam_id"),
			StringColumn("userid"),
			IntColumn("anonymousid"),
		)),

		Migration("feedbackforsits", Seq(
			StringColumn("id"),
			StringColumn("feedback_id"),
			StringColumn("status"),
			TimestampColumn("firstcreatedon"),
			TimestampColumn("lastinitialisedon"),
			TimestampColumn("dateofupload"),
			IntColumn("actualmarklastuploaded"),
			StringColumn("actualgradelastuploaded"),
			StringColumn("initialiser"),
			IntColumn("hib_version"),
		)),

		Migration("feedbacktemplate", Seq(
			StringColumn("id"),
			StringColumn("name"),
			StringColumn("description"),
			StringColumn("department_id"),
			StringColumn("attachment_id"),
		)),

		Migration("fileattachment", Seq(
			StringColumn("id"),
			StringColumn("name"),
			BooleanColumn("temporary"),
			StringColumn("feedback_id"),
			StringColumn("submission_id"),
			TimestampColumn("dateuploaded"),
			StringColumn("extension_id"),
			StringColumn("file_hash"),
			StringColumn("meetingrecord_id"),
			StringColumn("member_note_id"),
			StringColumn("uploadedby"),
		)),

		Migration("fileattachmenttoken", Seq(
			StringColumn("id"),
			TimestampColumn("expires"),
			TimestampColumn("date_used"),
			StringColumn("fileattachment_id"),
		)),

		Migration("formfield", Seq(
			StringColumn("id"),
			StringColumn("assignment_id"),
			StringColumn("name"),
			IntColumn("position"),
			StringColumn("label"),
			StringColumn("instructions"),
			StringColumn("fieldtype"),
			BooleanColumn("required"),
			StringColumn("properties"),
			StringColumn("context"),
			StringColumn("exam_id"),
		)),

		Migration("gradeboundary", Seq(
			StringColumn("id"),
			StringColumn("markscode"),
			StringColumn("grade"),
			IntColumn("minimummark"),
			IntColumn("maximummark"),
			StringColumn("signalstatus"),
		)),

		Migration("grantedpermission", Seq(
			StringColumn("id"),
			StringColumn("usergroup_id"),
			StringColumn("permission"),
			BooleanColumn("overridetype"),
			StringColumn("scope_type"),
			StringColumn("scope_id"),
			IntColumn("hib_version"),
		)),

		Migration("grantedrole", Seq(
			StringColumn("id"),
			StringColumn("usergroup_id"),
			StringColumn("custom_role_id"),
			StringColumn("builtinroledefinition"),
			StringColumn("scope_type"),
			StringColumn("scope_id"),
			IntColumn("hib_version"),
		)),

		Migration("job", Seq(
			StringColumn("id"),
			StringColumn("jobtype"),
			StringColumn("status"),
			StringColumn("data"),
			BooleanColumn("started"),
			BooleanColumn("finished"),
			BooleanColumn("succeeded"),
			IntColumn("progress"),
			TimestampColumn("createddate"),
			TimestampColumn("updateddate"),
			StringColumn("realuser"),
			StringColumn("apparentuser"),
			StringColumn("instance"),
		)),

		Migration("mark", Seq(
			StringColumn("id"),
			StringColumn("feedback_id"),
			StringColumn("uploaderid"),
			TimestampColumn("uploadeddate"),
			StringColumn("marktype"),
			IntColumn("mark"),
			StringColumn("grade"),
			StringColumn("reason"),
			StringColumn("comments"),
		)),

		Migration("marker_usergroup", Seq(
			StringColumn("id"),
			StringColumn("assignment_id"),
			StringColumn("markermap_id"),
			StringColumn("marker_uni_id"),
			StringColumn("discriminator"),
			StringColumn("exam_id"),
		)),

		Migration("markerfeedback", Seq(
			StringColumn("id"),
			IntColumn("mark"),
			StringColumn("state"),
			TimestampColumn("uploaded_date"),
			StringColumn("feedback_id"),
			StringColumn("grade"),
			StringColumn("rejectioncomments"),
			BooleanColumn("deleted"),
			StringColumn("marker"),
			StringColumn("stage"),
			TimestampColumn("updated_on"),
		)),

		Migration("markerfeedbackattachment", Seq(
			StringColumn("marker_feedback_id"),
			StringColumn("file_attachment_id"),
		)),

		Migration("markingdescriptor", Seq(
			StringColumn("id"),
			StringColumn("discriminator"),
			StringColumn("department_id"),
			IntColumn("min_mark"),
			IntColumn("max_mark"),
			StringColumn("text"),
		)),

		Migration("markscheme", Seq(
			StringColumn("id"),
			StringColumn("name"),
			StringColumn("department_id"),
			StringColumn("firstmarkers_id"),
			StringColumn("secondmarkers_id"),
			BooleanColumn("studentschoosemarker"),
			StringColumn("markingmethod"),
		)),

		Migration("meetingrecord", Seq(
			StringColumn("id"),
			StringColumn("relationship_id"),
			TimestampColumn("creation_date"),
			TimestampColumn("last_updated_date"),
			StringColumn("creator_id"),
			TimestampColumn("meeting_date"),
			StringColumn("title"),
			StringColumn("description"),
			StringColumn("meeting_format"),
			BooleanColumn("deleted"),
			StringColumn("discriminator"),
			BooleanColumn("missed"),
			StringColumn("missed_reason"),
			BooleanColumn("real_time"),
			TimestampColumn("meeting_end_date"),
			StringColumn("meeting_location"),
		)),

		Migration("meetingrecordapproval", Seq(
			StringColumn("id"),
			StringColumn("meetingrecord_id"),
			StringColumn("approver_id"),
			TimestampColumn("lastupdateddate"),
			StringColumn("comments"),
			StringColumn("approval_state"),
			TimestampColumn("creation_date"),
			StringColumn("approved_by"),
		)),

		Migration("meetingrecordrelationship", Seq(
			StringColumn("meeting_record_id"),
			StringColumn("relationship_id"),
		)),

		Migration("member", Seq(
			StringColumn("universityid"),
			StringColumn("userid"),
			StringColumn("firstname"),
			StringColumn("lastname"),
			StringColumn("email"),
			StringColumn("title"),
			StringColumn("fullfirstname"),
			StringColumn("gender"),
			StringColumn("nationality"),
			StringColumn("homeemail"),
			StringColumn("mobilenumber"),
			StringColumn("photo_id"),
			StringColumn("inuseflag"),
			TimestampColumn("inactivationdate"),
			StringColumn("groupname"),
			StringColumn("home_department_id"),
			DateColumn("dateofbirth"),
			BooleanColumn("teachingstaff"),
			StringColumn("home_address_id"),
			StringColumn("termtime_address_id"),
			TimestampColumn("lastupdateddate"),
			StringColumn("usertype"),
			IntColumn("hib_version"),
			StringColumn("phonenumber"),
			StringColumn("jobtitle"),
			StringColumn("mostsignificantcourse"),
			TimestampColumn("missingfromimportsince"),
			StringColumn("assistantsgroup_id"),
			BooleanColumn("tier4_visa_requirement"),
			StringColumn("disability"),
			StringColumn("timetable_hash"),
			StringColumn("settings"),
			BooleanColumn("deceased"),
			TimestampColumn("lastimportdate"),
			StringColumn("secondnationality"),
			StringColumn("disability_funding"),
		)),

		Migration("membernote", Seq(
			StringColumn("id"),
			StringColumn("memberid"),
			StringColumn("note"),
			StringColumn("title"),
			StringColumn("creatorid"),
			TimestampColumn("creationdate"),
			TimestampColumn("lastupdateddate"),
			BooleanColumn("deleted"),
			StringColumn("discriminator"),
			TimestampColumn("start_date"),
			TimestampColumn("end_date"),
		)),

		Migration("modeofattendance", Seq(
			StringColumn("code"),
			StringColumn("shortname"),
			StringColumn("fullname"),
			TimestampColumn("lastupdateddate"),
		)),

		Migration("module", Seq(
			StringColumn("id"),
			BooleanColumn("active"),
			StringColumn("code"),
			StringColumn("name"),
			StringColumn("department_id"),
			TimestampColumn("missingfromimportsince"),
			StringColumn("degreetype"),
			StringColumn("shortname"),
		)),

		Migration("moduleregistration", Seq(
			StringColumn("id"),
			StringColumn("scjcode"),
			StringColumn("modulecode"),
			IntColumn("academicyear"),
			BigDecimalColumn("cats"),
			StringColumn("assessmentgroup"),
			StringColumn("selectionstatuscode"),
			TimestampColumn("lastupdateddate"),
			IntColumn("hib_version"),
			StringColumn("occurrence"),
			IntColumn("agreedmark"),
			StringColumn("agreedgrade"),
			IntColumn("actualmark"),
			StringColumn("actualgrade"),
			BooleanColumn("deleted"),
			BooleanColumn("passfail"),
		)),

		Migration("moduleteachinginformation", Seq(
			StringColumn("id"),
			StringColumn("department_id"),
			StringColumn("module_id"),
			BigDecimalColumn("percentage"),
		)),

		Migration("monitoringcheckpoint", Seq(
			StringColumn("id"),
			StringColumn("point_id"),
			StringColumn("student_course_detail_id"),
			BooleanColumn("checked"),
			TimestampColumn("createddate"),
			StringColumn("createdby"),
			StringColumn("state"),
			TimestampColumn("updateddate"),
			StringColumn("updatedby"),
			BooleanColumn("autocreated"),
			StringColumn("student_id"),
		)),

		Migration("monitoringpoint", Seq(
			StringColumn("id"),
			StringColumn("point_set_id"),
			StringColumn("name"),
			BooleanColumn("defaultvalue"),
			TimestampColumn("createddate"),
			TimestampColumn("updateddate"),
			IntColumn("week"),
			BooleanColumn("senttoacademicoffice"),
			IntColumn("validfromweek"),
			IntColumn("requiredfromweek"),
			StringColumn("settings"),
			StringColumn("point_type"),
		)),

		Migration("monitoringpointreport", Seq(
			StringColumn("id"),
			StringColumn("student"),
			StringColumn("student_course_details_id"),
			StringColumn("student_course_year_details_id"),
			TimestampColumn("createddate"),
			TimestampColumn("pusheddate"),
			StringColumn("monitoringperiod"),
			IntColumn("academicyear"),
			IntColumn("missed"),
			StringColumn("reporter"),
		)),

		Migration("monitoringpointset", Seq(
			StringColumn("id"),
			StringColumn("route_id"),
			IntColumn("year"),
			TimestampColumn("createddate"),
			TimestampColumn("updateddate"),
			IntColumn("academicyear"),
			StringColumn("migratedto"),
		)),

		Migration("monitoringpointsettemplate", Seq(
			StringColumn("id"),
			TimestampColumn("createddate"),
			TimestampColumn("updateddate"),
			StringColumn("templatename"),
			IntColumn("position"),
		)),

		Migration("monitoringpointtemplate", Seq(
			StringColumn("id"),
			StringColumn("point_set_id"),
			StringColumn("name"),
			BooleanColumn("defaultvalue"),
			TimestampColumn("createddate"),
			TimestampColumn("updateddate"),
			IntColumn("week"),
			IntColumn("validfromweek"),
			IntColumn("requiredfromweek"),
			StringColumn("settings"),
			StringColumn("point_type"),
		)),

		Migration("nextofkin", Seq(
			StringColumn("id"),
			StringColumn("member_id"),
			StringColumn("address_id"),
			StringColumn("firstname"),
			StringColumn("lastname"),
			StringColumn("relationship"),
			StringColumn("eveningphone"),
			StringColumn("email"),
		)),

		Migration("normalcatsload", Seq(
			StringColumn("id"),
			IntColumn("academicyear"),
			StringColumn("routecode"),
			IntColumn("yearofstudy"),
			BigDecimalColumn("normalload"),
		)),

		Migration("originalityreport", Seq(
			StringColumn("id"),
			StringColumn("submission_id"),
			TimestampColumn("createddate"),
			IntColumn("similarity"),
			IntColumn("overlap"),
			IntColumn("student_overlap"),
			IntColumn("web_overlap"),
			IntColumn("publication_overlap"),
			StringColumn("attachment_id"),
			StringColumn("turnitin_id"),
			BooleanColumn("report_received"),
			TimestampColumn("lastsubmittedtoturnitin"),
			IntColumn("submittoturnitinretries"),
			TimestampColumn("filerequested"),
			TimestampColumn("lastreportrequest"),
			IntColumn("reportrequestretries"),
			StringColumn("lastturnitinerror"),
			TimestampColumn("nextsubmitattempt"),
			IntColumn("submitattempts"),
			TimestampColumn("submitteddate"),
			TimestampColumn("nextresponseattempt"),
			IntColumn("responseattempts"),
			TimestampColumn("responsereceived"),
			StringColumn("reporturl"),
			BigDecimalColumn("significance"),
			IntColumn("matchcount"),
			IntColumn("sourcecount"),
			StringColumn("urkundresponse"),
			StringColumn("urkundresponsecode"),
		)),

		Migration("outstandingstages", Seq(
			StringColumn("feedback_id"),
			StringColumn("stage"),
		)),

		Migration("roleoverride", Seq(
			StringColumn("id"),
			StringColumn("custom_role_definition_id"),
			StringColumn("permission"),
			BooleanColumn("overridetype"),
			IntColumn("hib_version"),
		)),

		Migration("route", Seq(
			StringColumn("id"),
			BooleanColumn("active"),
			StringColumn("code"),
			StringColumn("name"),
			StringColumn("degreetype"),
			StringColumn("department_id"),
			TimestampColumn("missingfromimportsince"),
			BooleanColumn("teachingdepartmentsactive"),
		)),

		Migration("routeteachinginformation", Seq(
			StringColumn("id"),
			StringColumn("department_id"),
			StringColumn("route_id"),
			BigDecimalColumn("percentage"),
		)),

		Migration("sitsstatus", Seq(
			StringColumn("code"),
			StringColumn("shortname"),
			StringColumn("fullname"),
			TimestampColumn("lastupdateddate"),
		)),

		Migration("smallgroup", Seq(
			StringColumn("id"),
			StringColumn("set_id"),
			StringColumn("name"),
			StringColumn("studentsgroup_id"),
			StringColumn("settings"),
			StringColumn("linked_dept_group_id"),
		)),

		Migration("smallgroupevent", Seq(
			StringColumn("id"),
			StringColumn("group_id"),
			StringColumn("weekranges"),
			IntColumn("day"),
			StringColumn("starttime"),
			StringColumn("endtime"),
			StringColumn("title"),
			StringColumn("location"),
			StringColumn("tutorsgroup_id"),
			StringColumn("relatedurl"),
			StringColumn("relatedurltitle"),
		)),

		Migration("smallgroupeventattendance", Seq(
			StringColumn("id"),
			StringColumn("occurrence_id"),
			StringColumn("universityid"),
			StringColumn("state"),
			TimestampColumn("updateddate"),
			StringColumn("updatedby"),
			BooleanColumn("added_manually"),
			StringColumn("replaces_attendance_id"),
		)),

		Migration("smallgroupeventoccurrence", Seq(
			StringColumn("id"),
			IntColumn("week"),
			StringColumn("event_id"),
		)),

		Migration("smallgroupset", Seq(
			StringColumn("id"),
			StringColumn("module_id"),
			IntColumn("academicyear"),
			StringColumn("name"),
			BooleanColumn("archived"),
			BooleanColumn("deleted"),
			StringColumn("group_format"),
			StringColumn("membersgroup_id"),
			StringColumn("allocation_method"),
			BooleanColumn("released_to_students"),
			BooleanColumn("released_to_tutors"),
			BooleanColumn("self_group_switching"),
			StringColumn("settings"),
			BooleanColumn("open_for_signups"),
			BooleanColumn("collect_attendance"),
			StringColumn("linked_dept_group_set_id"),
			StringColumn("default_weekranges"),
			StringColumn("default_tutorsgroup_id"),
			StringColumn("default_location"),
			BooleanColumn("email_students"),
			BooleanColumn("email_tutors"),
			IntColumn("default_day"),
			StringColumn("default_starttime"),
			StringColumn("default_endtime"),
		)),

		Migration("stagemarkers", Seq(
			StringColumn("id"),
			StringColumn("stage"),
			StringColumn("markers"),
			StringColumn("workflow_id"),
		)),

		Migration("studentcoursedetails", Seq(
			StringColumn("scjcode"),
			StringColumn("universityid"),
			StringColumn("sprcode"),
			StringColumn("routecode"),
			StringColumn("coursecode"),
			StringColumn("awardcode"),
			StringColumn("sprstatuscode"),
			StringColumn("levelcode"),
			DateColumn("begindate"),
			DateColumn("enddate"),
			DateColumn("expectedenddate"),
			IntColumn("courseyearlength"),
			BooleanColumn("mostsignificant"),
			TimestampColumn("lastupdateddate"),
			IntColumn("hib_version"),
			StringColumn("department_id"),
			StringColumn("latestyeardetails"),
			TimestampColumn("missingfromimportsince"),
			StringColumn("scjstatuscode"),
			StringColumn("reasonfortransfercode"),
			IntColumn("sprstartacademicyear"),
		)),

		Migration("studentcoursedetailsnote", Seq(
			StringColumn("code"),
			StringColumn("scjcode"),
			StringColumn("note"),
		)),

		Migration("studentcourseyeardetails", Seq(
			StringColumn("id"),
			StringColumn("scjcode"),
			IntColumn("scesequencenumber"),
			IntColumn("academicyear"),
			StringColumn("enrolmentstatuscode"),
			StringColumn("modeofattendancecode"),
			IntColumn("yearofstudy"),
			TimestampColumn("lastupdateddate"),
			IntColumn("hib_version"),
			StringColumn("moduleregistrationstatus"),
			TimestampColumn("missingfromimportsince"),
			StringColumn("enrolment_department_id"),
			BooleanColumn("cas_used"),
			BooleanColumn("tier4visa"),
			BooleanColumn("enrolledorcompleted"),
			StringColumn("overcatting"),
			BigDecimalColumn("agreedmark"),
			TimestampColumn("agreedmarkuploadeddate"),
			StringColumn("agreedmarkuploadedby"),
			StringColumn("routecode"),
			StringColumn("studylevel"),
			StringColumn("blockoccurrence"),
		)),

		Migration("studentrelationship", Seq(
			StringColumn("id"),
			StringColumn("relationship_type"),
			StringColumn("agent"),
			TimestampColumn("uploaded_date"),
			TimestampColumn("start_date"),
			TimestampColumn("end_date"),
			BigDecimalColumn("percentage"),
			StringColumn("scjcode"),
			StringColumn("agent_type"),
			StringColumn("external_agent_name"),
			BooleanColumn("terminated"),
			StringColumn("replacedby"),
		)),

		Migration("studentrelationshiptype", Seq(
			StringColumn("id"),
			StringColumn("urlpart"),
			StringColumn("description"),
			StringColumn("agentrole"),
			StringColumn("studentrole"),
			StringColumn("defaultsource"),
			BooleanColumn("defaultdisplay"),
			BooleanColumn("expected_ug"),
			BooleanColumn("expected_pgt"),
			BooleanColumn("expected_pgr"),
			IntColumn("sort_order"),
			StringColumn("defaultrdxtype"),
			BooleanColumn("expected_foundation"),
			BooleanColumn("expected_presessional"),
		)),

		Migration("studydetails", Seq(
			StringColumn("universityid"),
			IntColumn("hib_version"),
			StringColumn("sprcode"),
			StringColumn("sitscoursecode"),
			StringColumn("route_id"),
			StringColumn("study_department_id"),
			IntColumn("yearofstudy"),
			StringColumn("intendedaward"),
			DateColumn("begindate"),
			DateColumn("enddate"),
			DateColumn("expectedenddate"),
			StringColumn("fundingsource"),
			IntColumn("courseyearlength"),
			StringColumn("sprstatuscode"),
			StringColumn("enrolmentstatuscode"),
			StringColumn("modeofattendancecode"),
			StringColumn("scjcode"),
		)),

		Migration("studylevel", Seq(
			StringColumn("code"),
			StringColumn("shortname"),
			StringColumn("name"),
			TimestampColumn("lastupdateddate"),
			IntColumn("hib_version"),
		)),

		Migration("submission", Seq(
			StringColumn("id"),
			BooleanColumn("submitted"),
			TimestampColumn("submitted_date"),
			StringColumn("universityid"),
			StringColumn("userid"),
			StringColumn("assignment_id"),
			StringColumn("state"),
			StringColumn("plagiarisminvestigation"),
		)),

		Migration("submissionvalue", Seq(
			StringColumn("id"),
			StringColumn("name"),
			StringColumn("submission_id"),
			StringColumn("value_old"),
			StringColumn("feedback_id"),
			StringColumn("marker_feedback_id"),
			StringColumn("value"),
		)),

		Migration("syllabuspluslocation", Seq(
			StringColumn("id"),
			StringColumn("upstream_name"),
			StringColumn("name"),
			StringColumn("map_location_id"),
		)),

		Migration("upstreamassessmentgroup", Seq(
			StringColumn("id"),
			StringColumn("modulecode"),
			StringColumn("assessmentgroup"),
			IntColumn("academicyear"),
			StringColumn("occurrence"),
			StringColumn("sequence"),
		)),

		Migration("upstreamassessmentgroupmember", Seq(
			StringColumn("id"),
			StringColumn("group_id"),
			StringColumn("universityid"),
			IntColumn("position"),
			IntColumn("actualmark"),
			StringColumn("actualgrade"),
			IntColumn("agreedmark"),
			StringColumn("agreedgrade"),
			IntColumn("resitactualmark"),
			StringColumn("resitactualgrade"),
			IntColumn("resitagreedmark"),
			StringColumn("resitagreedgrade"),
		)),

		Migration("upstreamassignment", Seq(
			StringColumn("id"),
			StringColumn("modulecode"),
			StringColumn("assessmentgroup"),
			StringColumn("sequence"),
			StringColumn("name"),
			StringColumn("assessmenttype"),
			StringColumn("module_id"),
			BooleanColumn("in_use"),
			StringColumn("markscode"),
			IntColumn("weighting"),
		)),

		Migration("upstreammember", Seq(
			StringColumn("universityid"),
			StringColumn("userid"),
			StringColumn("firstname"),
			StringColumn("lastname"),
			StringColumn("email"),
		)),

		Migration("upstreammodulelist", Seq(
			StringColumn("code"),
			IntColumn("academicyear"),
			StringColumn("routecode"),
			IntColumn("yearofstudy"),
		)),

		Migration("upstreammodulelistentry", Seq(
			StringColumn("id"),
			StringColumn("listcode"),
			StringColumn("matchstring"),
		)),

		Migration("upstreamrouterule", Seq(
			StringColumn("id"),
			IntColumn("academicyear"),
			StringColumn("routecode"),
			StringColumn("levelcode"),
		)),

		Migration("upstreamrouteruleentry", Seq(
			StringColumn("id"),
			StringColumn("rule_id"),
			StringColumn("listcode"),
			BigDecimalColumn("min_cats"),
			BigDecimalColumn("max_cats"),
			IntColumn("min_modules"),
			IntColumn("max_modules"),
		)),

		Migration("usergroup", Seq(
			StringColumn("id"),
			StringColumn("basewebgroup"),
			BooleanColumn("universityids"),
		)),

		Migration("usergroupexclude", Seq(
			StringColumn("group_id"),
			StringColumn("usercode"),
		)),

		Migration("usergroupinclude", Seq(
			StringColumn("group_id"),
			StringColumn("usercode"),
		)),

		Migration("usergroupstatic", Seq(
			StringColumn("group_id"),
			StringColumn("usercode"),
		)),

		Migration("usersettings", Seq(
			StringColumn("id"),
			StringColumn("userid"),
			StringColumn("settings"),
		)),

		Migration("entityreference", Seq(
			StringColumn("id"),
			StringColumn("entity_type"),
			StringColumn("entity_id"),
			StringColumn("notification_id"),
		), restriction = Some(
			"""
				|where
				|  notification_id in (select id from notification where created >= trunc(sysdate - 30)) or
				|  id in (select target_id from notification where created >= trunc(sysdate - 30)) or
				|  id in (select target_id from scheduled_notification where scheduled_date >= trunc(sysdate - 30)) or
				|  id in (select target_id from scheduledtrigger where scheduled_date >= trunc(sysdate - 30))
			""".stripMargin
		)),

		Migration("notification", Seq(
			StringColumn("id"),
			StringColumn("notification_type"),
			StringColumn("agent"),
			TimestampColumn("created"),
			StringColumn("target_id"),
			StringColumn("settings"),
			StringColumn("recipientuserid"),
			StringColumn("recipientuniversityid"),
			StringColumn("priority"),
			BooleanColumn("listeners_processed"),
		), restriction = Some("where created >= trunc(sysdate - 30)")),

		Migration("recipientnotificationinfo", Seq(
			StringColumn("id"),
			StringColumn("notification_id"),
			StringColumn("recipient"),
			BooleanColumn("dismissed"),
			BooleanColumn("email_sent"),
			TimestampColumn("attemptedat"),
		), restriction = Some("where notification_id in (select id from notification where created >= trunc(sysdate - 30))")),

		Migration("scheduled_notification", Seq(
			StringColumn("id"),
			StringColumn("notification_type"),
			TimestampColumn("scheduled_date"),
			StringColumn("target_id"),
			BooleanColumn("completed"),
		), restriction = Some("where scheduled_date >= trunc(sysdate - 30)")),

		Migration("scheduledtrigger", Seq(
			StringColumn("id"),
			StringColumn("trigger_type"),
			TimestampColumn("scheduled_date"),
			StringColumn("target_id"),
			TimestampColumn("completed_date"),
		), restriction = Some("where scheduled_date >= trunc(sysdate - 30)")),

		Migration("auditevent", Seq(
			IntColumn("id"),
			TimestampColumn("eventdate"),
			StringColumn("eventtype"),
			StringColumn("eventstage"),
			StringColumn("real_user_id"),
			StringColumn("masquerade_user_id"),
			StringColumn("data"),
			StringColumn("eventid"),
			StringColumn("ip_address"),
			StringColumn("user_agent"),
			BooleanColumn("read_only"),
		), restriction = Some("where eventdate >= trunc(sysdate - 30)")),

		Migration("entityreference", Seq(
			StringColumn("id"),
			StringColumn("entity_type"),
			StringColumn("entity_id"),
			StringColumn("notification_id"),
		), restriction = Some(
			"""
				|where
				|  notification_id not in (select id from notification where created >= trunc(sysdate - 30)) and
				|  id not in (select target_id from notification where created >= trunc(sysdate - 30)) and
				|  id not in (select target_id from scheduled_notification where scheduled_date >= trunc(sysdate - 30)) and
				|  id not in (select target_id from scheduledtrigger where scheduled_date >= trunc(sysdate - 30))
			""".stripMargin
		)),

		Migration("notification", Seq(
			StringColumn("id"),
			StringColumn("notification_type"),
			StringColumn("agent"),
			TimestampColumn("created"),
			StringColumn("target_id"),
			StringColumn("settings"),
			StringColumn("recipientuserid"),
			StringColumn("recipientuniversityid"),
			StringColumn("priority"),
			BooleanColumn("listeners_processed"),
		), restriction = Some("where created < trunc(sysdate - 30)")),

		Migration("recipientnotificationinfo", Seq(
			StringColumn("id"),
			StringColumn("notification_id"),
			StringColumn("recipient"),
			BooleanColumn("dismissed"),
			BooleanColumn("email_sent"),
			TimestampColumn("attemptedat"),
		), restriction = Some("where notification_id not in (select id from notification where created >= trunc(sysdate - 30))")),

		Migration("scheduled_notification", Seq(
			StringColumn("id"),
			StringColumn("notification_type"),
			TimestampColumn("scheduled_date"),
			StringColumn("target_id"),
			BooleanColumn("completed"),
		), restriction = Some("where scheduled_date < trunc(sysdate - 30)")),

		Migration("scheduledtrigger", Seq(
			StringColumn("id"),
			StringColumn("trigger_type"),
			TimestampColumn("scheduled_date"),
			StringColumn("target_id"),
			TimestampColumn("completed_date"),
		), restriction = Some("where scheduled_date < trunc(sysdate - 30)")),

		Migration("auditevent", Seq(
			IntColumn("id"),
			TimestampColumn("eventdate"),
			StringColumn("eventtype"),
			StringColumn("eventstage"),
			StringColumn("real_user_id"),
			StringColumn("masquerade_user_id"),
			StringColumn("data"),
			StringColumn("eventid"),
			StringColumn("ip_address"),
			StringColumn("user_agent"),
			BooleanColumn("read_only"),
		), restriction = Some("where eventdate < trunc(sysdate - 30)")),
	)
}

@Controller
@RequestMapping(value = scala.Array("/sysadmin/db-migrate"))
class DatabaseMigrationController extends BaseSysadminController {

	val newDataSource: DataSource = Wire.named[DataSource]("dataSource")

	@RequestMapping
	def form(): Mav = Mav("sysadmin/db-migrate")

	private def logAndWrite(msg: String)(implicit writer: PrintWriter): Unit = {
		logger.info(msg)
		writer.println(s"[${OffsetDateTime.now()}] $msg")
	}

	@PostMapping
	def migrate(
		@RequestParam jdbcUrl: String,
		@RequestParam username: String,
		@RequestParam password: String,
		response: HttpServletResponse
	): Unit = {
		// Create the Oracle datasource
		val oldDataSource = new SimpleDriverDataSource()
		oldDataSource.setDriverClass(classOf[oracle.jdbc.OracleDriver])
		oldDataSource.setUrl(jdbcUrl)
		oldDataSource.setUsername(username)
		oldDataSource.setPassword(password)

		val oldConnection = oldDataSource.getConnection
		oldConnection.setReadOnly(true)

		val newConnection = newDataSource.getConnection
		newConnection.setAutoCommit(false)

		implicit val writer: PrintWriter = response.getWriter

		// Do the AuditEvent sequence first
		logAndWrite(s"Migrating auditevent_seq")
		logAndWrite("------------")

		val st = oldConnection.createStatement()
		val rs = st.executeQuery("select auditevent_seq.nextval from dual")
		rs.next()
		val nextVal = rs.getLong(1)

		val update = newConnection.createStatement()
		update.executeQuery(s"SELECT setval('auditevent_seq', $nextVal, false)")
		update.close()

		logAndWrite(s"Set auditevent_seq next value to $nextVal")

		rs.close()
		st.close()

		writer.println()

		// Do all the truncating first
		DatabaseMigrationController.mappings.map(_.tableName).distinct.foreach { tableName =>
			val truncate = newConnection.createStatement()
			truncate.executeUpdate(s"truncate $tableName cascade")
			newConnection.commit()
			truncate.close()
		}

		DatabaseMigrationController.mappings.foreach { mapping =>
			logAndWrite(s"Migrating ${mapping.tableName}")
			logAndWrite("------------")

			val insert = newConnection.prepareStatement(
				s"""
					|insert into ${mapping.tableName}
					|(${mapping.migrations.map(_.columnName).mkString(",")}) values
					|(${mapping.migrations.map(_ => "?").mkString(",")})
				""".stripMargin)

			// Get the total count
			val st = oldConnection.createStatement()
			val rs = st.executeQuery(s"SELECT count(*) FROM ${mapping.tableName} ${mapping.restriction.getOrElse("")}")
			rs.next()
			val count = rs.getLong(1)
			rs.close()
			st.close()

			val statement = oldConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
			val results = statement.executeQuery(
				s"SELECT ${mapping.migrations.map(_.columnName).mkString(",")} FROM ${mapping.tableName} ${mapping.restriction.getOrElse("")}"
			)

			logAndWrite(s"$count rows to insert")

			def perc(i: Int): Long =
				if (count > 0) i * 100 / count
				else 100

			var i = 0
			while (results.next()) {
				mapping.migrations.zipWithIndex.foreach { case (migration, index) =>
					migration.process(index + 1, results, insert)
				}
				insert.addBatch()

				i = i + 1
				if (i % 1000 == 0) {
					insert.executeBatch()
					logAndWrite(s"[${perc(i)}%] Inserted $i rows")
					insert.clearBatch()
				}
			}

			insert.executeBatch()
			logAndWrite(s"[${perc(i)}%] Inserted $i rows")
			insert.clearBatch()

			results.close()
			statement.close()
			insert.close()
			newConnection.commit()

			writer.println()
		}

		oldConnection.close()
		newConnection.close()
	}

}
