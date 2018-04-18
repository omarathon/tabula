package uk.ac.warwick.tabula.commands.profiles.admin.timetables

import java.io.IOException

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpResponse, HttpStatus}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.helpers.ApacheHttpClientUtils
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{JsonObjectMapperFactory, RequestFailedException}

import scala.util.{Failure, Success, Try}

object TimetableCheckerCommand {
	type Result = String
	type Command = Appliable[Result]

	def apply(): Command =
		new TimetableCheckerCommandInternal()
			with ComposableCommand[String]
			with TimetableCheckerCommandPermissions
			with AutowiringCelcatConfigurationComponent
			with AutowiringApacheHttpClientComponent
			with Unaudited
}

class TimetableCheckerCommandInternal() extends CommandInternal[String] with TimetableCheckerCommandRequest {
	self: CelcatConfigurationComponent
		with ApacheHttpClientComponent =>

	val jsonMapper: ObjectMapper = JsonObjectMapperFactory.instance

	def applyInternal(): String = {
		val wbsConfiguration: CelcatDepartmentConfiguration = celcatConfiguration.wbsConfiguration

		val uriBuilder = new URIBuilder(wbsConfiguration.baseUri)
		uriBuilder.setPath(s"/$warwickUniId")
		uriBuilder.addParameter("forcebasic", "true")

		val uri = uriBuilder.build()

		val req = new HttpGet(uri)
		req.setHeader(ApacheHttpClientUtils.basicAuthHeader(new UsernamePasswordCredentials(wbsConfiguration.credentials.username, wbsConfiguration.credentials.password)))

		val handler: ResponseHandler[String] = new BasicResponseHandler() {
			override def handleResponse(response: HttpResponse): String = {
				val jsonString = super.handleResponse(response)
				val jsonObject = if (jsonMapper != null) jsonMapper.readValue(jsonString, classOf[List[Map[String, Any]]])
				jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)
			}
		}

		Try(httpClient.execute(req, handler)) match {
			case Success(jsonData) => jsonData
			case Failure(e) => throw new RequestFailedException("The WBS timetabling service could not be reached", e)
		}
	}
}

trait TimetableCheckerCommandRequest {
	var warwickUniId: String = _
}

trait TimetableCheckerCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Timetabling.Checker)
	}
}