package uk.ac.warwick.tabula.home

import dispatch.classic._
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import dispatch.classic.thread.ThreadSafeHttpClient
import uk.ac.warwick.tabula.FunctionalTestProperties


trait FixturesDriver {

	lazy val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	def createModule(departmentCode: String, code: String, name: String) {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/module"
		val req = url(uri).POST << Map("departmentCode" -> departmentCode, "code" -> code, "name" -> name)
		http.when(_==200)(req >| )
	}

	def createSmallGroupSet(moduleCode:String, groupSetName:String, groupCount:Int= 1, formatName:String="tutorial", allocationMethodName:String="M" +
		"anual"){
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/fixtures/create/groupset"
		val req = url(uri).POST << Map(
			"moduleCode" -> moduleCode,
			"groupSetName" -> groupSetName,
		  "formatName"->formatName,
		  "allocationMethodName"->allocationMethodName,
		  "groupCount"->groupCount.toString
		)
		http.when(_==200)(req >| )
	}




}

