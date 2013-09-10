package uk.ac.warwick.tabula.home

import dispatch.classic._
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import dispatch.classic.thread.ThreadSafeHttpClient
import uk.ac.warwick.tabula.FunctionalTestProperties
import scala.util.parsing.json.JSON
import scala.language.postfixOps


trait FixturesDriver {

	lazy val http: Http = new Http with thread.Safety with NoLogging {

		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
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

	def createRoute(routeCode:String, departmentCode:String, routeName:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/route"
		val req = url(uri).POST << Map(
			"routeCode" -> routeCode,
			"departmentCode"->departmentCode,
		  "routeName"->routeName)
		http.when(_==200)(req >|)

	}


	def createCourse(courseCode:String, courseName:String ){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/course"
		val req = url(uri).POST << Map(
			"courseCode" -> courseCode,
			"courseName"->courseName)
		http.when(_==200)(req >|)

	}

}

