package uk.ac.warwick.tabula.home

import dispatch.classic._
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import dispatch.classic.thread.ThreadSafeHttpClient
import uk.ac.warwick.tabula.{LoginDetails, FunctionalTestProperties}
import scala.util.parsing.json.JSON
import scala.language.postfixOps
import org.joda.time.DateTime


trait FixturesDriver {

	lazy val http: Http = new Http with thread.Safety with NoLogging {

		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	def updateExtensionSettings(departmentCode: String, allow: Boolean = true, managerUserId: String = "") = {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/update/extensionSettings"
		val req = url(uri).POST << Map("departmentCode" -> departmentCode, "allow" -> allow.toString, "userId" -> managerUserId)
		http.when(_==200)(req >| )
	}

	def createModule(departmentCode: String, code: String, name: String) {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/module"
		val req = url(uri).POST << Map("departmentCode" -> departmentCode, "code" -> code, "name" -> name)
		http.when(_==200)(req >| )
	}

	def createSmallGroupSet(moduleCode:String,
													groupSetName:String,
													groupCount:Int= 1,
													formatName:String="tutorial",
													allocationMethodName:String="Manual",
													openForSignups:Boolean = true,
													maxGroupSize:Int = 0,
													releasedToStudents:Boolean = true,
													allowSelfGroupSwitching:Boolean  = true):String  = {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/groupset"
		val req = url(uri).POST << Map(
			"moduleCode" -> moduleCode,
			"groupSetName" -> groupSetName,
		  "formatName"->formatName,
		  "allocationMethodName"->allocationMethodName,
		  "groupCount"->groupCount.toString,
		  "openForSignups"->openForSignups.toString,
		  "releasedToStudents"->releasedToStudents.toString,
		  "maxGroupSize"->maxGroupSize.toString,
		  "allowSelfGroupSwitching"->allowSelfGroupSwitching.toString
		)
		val resp = http.when(_==200)(req as_str)

		val id = JSON.parseFull(resp).get.asInstanceOf[Map[String,Any]]("id").toString
		id
	}

	def createSmallGroupEvent(setId: String,title: String, weekRange:String="1") {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/groupEvent"
		val req = url(uri).POST << Map("setId" -> setId, "title" -> title, "weekRange"->weekRange)
		http.when(_==200)(req >| )
	}


  def addStudentToGroupSet(studentUserId: String, setId:String){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/groupsetMembership"
		val req = url(uri).POST << Map(
			"groupSetId" -> setId,
			"userId" -> studentUserId)
		http.when(_==200)(req >|)
	}

	def addStudentToGroup(studentUserId: String, setId:String, groupName:String){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/groupMembership"
		val req = url(uri).POST << Map(
			"groupSetId" -> setId,
			"userId" -> studentUserId,
		  "groupName"->groupName)
		http.when(_==200)(req >|)
	}

	def createStudentMember(userId:String,
													genderCode:String = "M",
													routeCode:String="",
													yearOfStudy:Int=1,
													courseCode:String="",
													deptCode:String=""){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/studentMember"
		val req = url(uri).POST << Map(
			"userId" -> userId,
			"genderCode"->genderCode,
		  "yearOfStudy"->yearOfStudy.toString,
		  "routeCode"->routeCode,
		  "courseCode"->courseCode,
		  "deptCode"->deptCode
		)
		http.when(_==200)(req >|)
	}

	def updateAssignment(deptCode:String, assignmentName:String, openDate:Option[DateTime] = None, closeDate:Option[DateTime] = None){
		val datesToUpdate:Seq[(String,String)] = Seq("openDate"->openDate,"closeDate"->closeDate).map(t=>t._2 match {
			case None=>None
			case Some(d)=>Some(t._1, d.toString("dd-MMM-yyyy HH:mm:ss"))
		}).flatten
		val params:Seq[(String,String)] = Seq("deptCode"->deptCode,"assignmentName"->assignmentName) ++ datesToUpdate
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/update/assignment"
		val req = url(uri).POST << Map(
			params :_*
		)
		http.when(_==200)(req >|)
	}

	def createExtension(userId: String, assignmentId: String, approved: Boolean) {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/extension"
		val req = url(uri).POST << Map(
			"userId" -> userId,
			"assignmentId" -> assignmentId,
			"approved" -> approved.toString)
		http.when(_==200)(req >|)
	}

	def createRoute(routeCode:String, departmentCode:String, routeName:String, degreeType:String="UG" ){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/route"
		val req = url(uri).POST << Map(
			"routeCode" -> routeCode,
			"departmentCode"->departmentCode,
		  "routeName"->routeName,
		  "degreeType"->degreeType)
		http.when(_==200)(req >|)
	}


	def createCourse(courseCode:String, courseName:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/course"
		val req = url(uri).POST << Map(
			"courseCode" -> courseCode,
			"courseName"->courseName)
		http.when(_==200)(req >|)
	}

	def registerStudentsOnModule(students:Seq[LoginDetails], moduleCode:String){
		val uniIds = students.map(_.warwickId).mkString(",")
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/moduleRegistration"
		val req = url(uri).POST << Map(
			"universityIds" -> uniIds,
			"moduleCode" -> moduleCode)
		http.when(_==200)(req >|)
	}

	def createStudentRelationship(student:LoginDetails, agent:LoginDetails, relationshipType:String = "tutor"){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/relationship"
		val req = url(uri).POST << Map(
			"studentUniId" -> student.warwickId,
			"agent"->agent.warwickId,
		  "relationshipType"->relationshipType)
		http.when(_==200)(req >|)
	}

}

