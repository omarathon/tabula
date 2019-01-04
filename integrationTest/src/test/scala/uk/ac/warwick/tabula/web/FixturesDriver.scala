package uk.ac.warwick.tabula.web

import org.apache.http.HttpStatus
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.ApacheHttpClientUtils
import uk.ac.warwick.tabula.{AcademicYear, FunctionalTestProperties, LoginDetails}

import scala.language.postfixOps
import scala.util.parsing.json.JSON

trait FixturesDriver extends SimpleHttpFetching {

	def updateExtensionSettings(departmentCode: String, allow: Boolean = true, managerUserId: String = ""): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/update/extensionSettings"
		val req =
			RequestBuilder.post(uri)
				.addParameter("departmentCode", departmentCode)
				.addParameter("allow", allow.toString)
				.addParameter("userId", managerUserId)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createModule(departmentCode: String, code: String, name: String) {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/module"

		val req =
			RequestBuilder.post(uri)
				.addParameter("departmentCode", departmentCode)
				.addParameter("code", code)
				.addParameter("name", name)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createSmallGroupSet(
		moduleCode:String,
		groupSetName:String,
		groupCount:Int= 1,
		formatName:String="tutorial",
		allocationMethodName:String="Manual",
		openForSignups:Boolean = true,
		maxGroupSize:Int = 0,
		releasedToStudents:Boolean = true,
		allowSelfGroupSwitching:Boolean  = true,
		academicYear: String
	):String  = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupset"

		val req =
			RequestBuilder.post(uri)
				.addParameter("moduleCode", moduleCode)
				.addParameter("groupSetName", groupSetName)
				.addParameter("formatName", formatName)
				.addParameter("allocationMethodName", allocationMethodName)
				.addParameter("groupCount", groupCount.toString)
				.addParameter("openForSignups", openForSignups.toString)
				.addParameter("releasedToStudents", releasedToStudents.toString)
				.addParameter("maxGroupSize", maxGroupSize.toString)
				.addParameter("allowSelfGroupSwitching", allowSelfGroupSwitching.toString)
				.addParameter("academicYear", academicYear)

		val resp = httpClient.execute(req.build(), new BasicResponseHandler)

		val id = JSON.parseFull(resp).get.asInstanceOf[Map[String,Any]]("id").toString
		id
	}

	def createSmallGroupEvent(setId: String,title: String, weekRange:String="1") {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupEvent"

		val req =
			RequestBuilder.post(uri)
				.addParameter("setId", setId)
				.addParameter("title", title)
				.addParameter("weekRange", weekRange)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}


  def addStudentToGroupSet(studentUserId: String, setId:String){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupsetMembership"

		val req =
			RequestBuilder.post(uri)
				.addParameter("groupSetId", setId)
				.addParameter("userId", studentUserId)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def addStudentToGroup(studentUserId: String, setId:String, groupName:String){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/groupMembership"

		val req =
			RequestBuilder.post(uri)
				.addParameter("groupSetId", setId)
				.addParameter("userId", studentUserId)
				.addParameter("groupName", groupName)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createStudentMember(
		userId: String,
		genderCode: String = "M",
		routeCode: String="",
		yearOfStudy: Int=1,
		courseCode: String="",
		deptCode: String="",
		academicYear: String = "2014"
	){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/studentMember"

		val req =
			RequestBuilder.post(uri)
				.addParameter("userId", userId)
				.addParameter("genderCode", genderCode)
				.addParameter("yearOfStudy", yearOfStudy.toString)
				.addParameter("routeCode", routeCode)
				.addParameter("courseCode", courseCode)
				.addParameter("deptCode", deptCode)
				.addParameter("academicYear", academicYear)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createStaffMember(userId:String, genderCode:String = "M", deptCode:String){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/staffMember"

		val req =
			RequestBuilder.post(uri)
				.addParameter("userId", userId)
				.addParameter("genderCode", genderCode)
				.addParameter("deptCode", deptCode)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def updateAssignment(deptCode:String, assignmentName:String, openDate:Option[DateTime] = None, closeDate:Option[DateTime] = None){
		val datesToUpdate: Seq[(String,String)] = Seq("openDate" -> openDate, "closeDate" -> closeDate).flatMap(t => t._2 match {
			case None => None
			case Some(d) => Some(t._1, d.toString("dd-MMM-yyyy HH:mm:ss"))
		})
		val params: Seq[(String,String)] = Seq("deptCode" -> deptCode, "assignmentName" -> assignmentName) ++ datesToUpdate
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/update/assignment"

		val req = RequestBuilder.post(uri)
		params.foreach { case (k, v) => req.addParameter(k, v) }

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createExtension(userId: String, assignmentId: String, approved: Boolean) {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/extension"

		val req =
			RequestBuilder.post(uri)
				.addParameter("userId", userId)
				.addParameter("assignmentId", assignmentId)
				.addParameter("approved", approved.toString)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createRoute(routeCode:String, departmentCode:String, routeName:String, degreeType:String="UG" ){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/route"

		val req =
			RequestBuilder.post(uri)
				.addParameter("routeCode", routeCode)
				.addParameter("departmentCode", departmentCode)
				.addParameter("routeName", routeName)
				.addParameter("degreeType", degreeType)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}


	def createCourse(courseCode:String, courseName:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/course"

		val req =
			RequestBuilder.post(uri)
				.addParameter("courseCode", courseCode)
				.addParameter("courseName", courseName)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def registerStudentsOnModule(students: Seq[LoginDetails], moduleCode: String, academicYear: Option[String] = None) {
		val uniIds = students.map(_.warwickId).mkString(",")
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/moduleRegistration"

		val req =
			RequestBuilder.post(uri)
				.addParameter("universityIds", uniIds)
				.addParameter("moduleCode", moduleCode)
				.addParameter("academicYear", academicYear.getOrElse(AcademicYear.now().startYear.toString))

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createStudentRelationship(student:LoginDetails, agent:LoginDetails, relationshipType:String = "tutor"){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/relationship"

		val req =
			RequestBuilder.post(uri)
				.addParameter("studentUniId", student.warwickId)
				.addParameter("agent", agent.warwickId)
				.addParameter("relationshipType", relationshipType)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createMonitoringPointSet(routeCode:String, pointCount:Int, academicYear:String, yearOption:Option[Int]){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/monitoringPointSet"

		val req =
			RequestBuilder.post(uri)
				.addParameter("routeCode", routeCode)
				.addParameter("pointCount", pointCount.toString)
				.addParameter("academicYear", academicYear)

		if (yearOption.nonEmpty)
			req.addParameter("year", yearOption.get.toString)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createAttendanceMonitoringScheme(deptCode:String, pointCount:Int, academicYear:String, warwickId:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/attendanceMonitoringScheme"

		val req =
			RequestBuilder.post(uri)
				.addParameter("deptCode", deptCode)
				.addParameter("pointCount", pointCount.toString)
				.addParameter("academicYear", academicYear)
				.addParameter("warwickId", warwickId)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createAssessmentComponent(departmentCode: String, moduleCode: String, name: String, assessmentGroup: String = "A", sequence: String = "A01") {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/assessmentComponent"

		val req =
			RequestBuilder.post(uri)
				.addParameter("moduleCode", moduleCode)
				.addParameter("assessmentGroup", assessmentGroup)
				.addParameter("sequence", sequence)
				.addParameter("departmentCode", departmentCode)
				.addParameter("name", name)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createUpstreamAssessmentGroup(moduleCode: String, universityIds: Seq[String], assessmentGroup: String = "A", occurrence: String = "A") {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/upstreamAssessmentGroup"

		val req =
			RequestBuilder.post(uri)
				.addParameter("moduleCode", moduleCode)
				.addParameter("assessmentGroup", assessmentGroup)
				.addParameter("occurrence", occurrence)

		universityIds.zipWithIndex.foreach { case (universityId, index) =>
			req.addParameter(s"universityIds[$index]", universityId)
		}

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createPremarkedAssignment(moduleCode: String): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/premarkedAssignment"

		val req =
			RequestBuilder.post(uri)
				.addParameter("moduleCode", moduleCode)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createPremarkedCM2Assignment(moduleCode: String): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/premarkedCM2Assignment"

		val req =
			RequestBuilder.post(uri)
				.addParameter("moduleCode", moduleCode)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

	def createMemberNote(memberId: String, creatorId: String, note: String, title: String = ""): Unit = {
		val uri = FunctionalTestProperties.SiteRoot + "/fixtures/create/memberNote"

		val req =
			RequestBuilder.post(uri)
				.addParameter("memberId", memberId)
				.addParameter("creatorId", creatorId)
				.addParameter("note", note)
				.addParameter("title", title)

		httpClient.execute(
			req.build(),
			ApacheHttpClientUtils.statusCodeFilteringHandler(HttpStatus.SC_OK)(EntityUtils.consumeQuietly)
		)
	}

}