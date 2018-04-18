package uk.ac.warwick.tabula.web.controllers.profiles

import org.apache.http.HttpEntity
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.AbstractResponseHandler
import org.apache.http.util.EntityUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationUtils
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.{Logging, PhoneNumberFormatter}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent, AutowiringTrustedApplicationsManagerComponent, TrustedApplicationsManagerComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.web.UriBuilder

import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

trait PeopleSearchData extends Logging {

	self: ApacheHttpClientComponent with TrustedApplicationsManagerComponent =>

	var peoplesearchUrl: String = Wire.property("${peoplesearch.api}")

	def getDataFromPeoplesearch(onBehalfOf: String, universityId:String): Map[String, String] = {
		val handler: ResponseHandler[Map[String, String]] =
			new AbstractResponseHandler[Map[String, String]] {
				override def handleEntity(entity: HttpEntity): Map[String, String] = {
					val json = EntityUtils.toString(entity)
					JSON.parseFull(json) match {
						//collecting simple properties only (phone, room are defined as simple ones and that is what we are interested in)
						case Some(json: Map[String, Any]@unchecked) =>
							json.get("data") match {
								case Some(jsonData: List[Map[String, Any]]@unchecked) =>
									jsonData.flatten.toMap.collect { case(propertyName:String, propertyValue:String) => (propertyName,propertyValue) }
								case _ => Map[String,String]()
							}
						case _ => throw new RuntimeException("Could not parse JSON")
					}
				}
			}

		val queryPara = s"membershipDetails.universityId:$universityId AND sequenceNumber:0"
		val luceneQueryPara = "true"
		val endPointUrl =
			UriBuilder.parse(peoplesearchUrl)
				.addQueryParameter("luceneQueryType", luceneQueryPara)
				.addQueryParameter("query", queryPara)
				.toString

		val req = new HttpGet(endPointUrl)
		TrustedApplicationUtils.signRequest(applicationManager.getCurrentApplication, onBehalfOf, req)

		Try(httpClient.execute(req, handler)) match {
			case Success(jsonData) => jsonData
			case Failure(e) =>
				logger.warn(s"Request for ${req.getURI.toString} failed", e)
				Map[String, String]()
		}
	}

}

object PeoplesearchCommand {
	def apply(member: Member, user:CurrentUser) =
		new PeoplesearchCommandInternal(member, user)
			with AutowiringApacheHttpClientComponent
			with AutowiringTrustedApplicationsManagerComponent
			with ComposableCommand[Map[String, String]]
			with PeoplesearchPermissions
			with PeoplesearchCommandState
			with PeopleSearchData
			with Unaudited
			with ReadOnly
}

class PeoplesearchCommandInternal(val member: Member, val user: CurrentUser) extends CommandInternal[Map[String, String]] {

	self: PeopleSearchData =>

	override def applyInternal(): Map[String, String] = {
		val data = getDataFromPeoplesearch(user.userId, member.id)
		data.transform { (key, value) =>
			if (key.equals("extensionNumberWithExternal")) PhoneNumberFormatter.format(value) else value
		}
	}
}

trait PeoplesearchPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: PeoplesearchCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Core, mandatory(member))
	}
}

trait PeoplesearchCommandState {
	def member: Member
}
