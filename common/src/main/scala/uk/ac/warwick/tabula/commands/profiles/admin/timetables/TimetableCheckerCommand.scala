package uk.ac.warwick.tabula.commands.profiles.admin.timetables

import org.apache.http.client.methods.RequestBuilder
import org.springframework.validation.Errors
import play.api.libs.json.Json
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.profiles.admin.timetables.TimetableCheckerCommand._
import uk.ac.warwick.tabula.helpers.ApacheHttpClientUtils
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent, AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, RequestFailedException}

import scala.util.{Failure, Success, Try}
import scala.xml.PrettyPrinter

object TimetableCheckerCommand {
  case class Result(syllabusPlusFeed: String, wbsFeed: String)
  type Command = Appliable[Result] with SelfValidating

  val XmlPrettyPrinter = new PrettyPrinter(120, 2)

  def apply(): Command =
    new TimetableCheckerCommandInternal()
      with ComposableCommand[Result]
      with TimetableCheckerCommandPermissions
      with TimetableCheckerCommandValidation
      with AutowiringScientiaConfigurationComponent
      with AutowiringCelcatConfigurationComponent
      with AutowiringApacheHttpClientComponent
      with AutowiringProfileServiceComponent
      with Unaudited
}

class TimetableCheckerCommandInternal() extends CommandInternal[Result] with TimetableCheckerCommandRequest {
  self: ScientiaConfigurationComponent
    with CelcatConfigurationComponent
    with ApacheHttpClientComponent
    with ProfileServiceComponent =>

  private def fetchSyllabusPlusFeed(): String = {
    val perYearUris = scientiaConfiguration.perYearUris

    val studentUris: Seq[(String, AcademicYear)] = perYearUris.map {
      case (uri, year) => (uri + "?StudentXML", year)
    }
    val staffUris: Seq[(String, AcademicYear)] = perYearUris.map {
      case (uri, year) => (uri + "?StaffXML", year)
    }

    // Guarded by validation so .get is safe
    val member = profileService.getMemberByUniversityIdStaleOrFresh(warwickUniId).get
    val uris =
      if (member.isStudent) studentUris
      else if (member.isStaff) staffUris
      else throw new RequestFailedException(s"The university ID $warwickUniId was not a staff or student (${member.userType})", new IllegalArgumentException)

    uris.map { case (uri, year) =>
      // add ?p0={param} to the URL's get parameters
      val req =
        RequestBuilder.get(uri)
          .addParameter("p0", warwickUniId)
          .build()

      Try(httpClient.execute(req, ApacheHttpClientUtils.xmlResponseHandler(XmlPrettyPrinter.format(_)))) match {
        case Success(xml) => s"${year.toString}:\n$xml"
        case Failure(e) => throw new RequestFailedException("The Syllabus+ timetabling service could not be reached", e)
      }
    }.mkString("\n\n")
  }

  private def fetchWbsFeed(): String = {
    val wbsConfiguration: CelcatDepartmentConfiguration = celcatConfiguration.wbsConfiguration

    val req =
      RequestBuilder.get(s"${wbsConfiguration.baseUri}/$warwickUniId")
        .addParameter("forcebasic", "true")
        .setHeader(ApacheHttpClientUtils.basicAuthHeader(wbsConfiguration.credentials))
        .build()

    Try(httpClient.execute(req, ApacheHttpClientUtils.jsonResponseHandler(Json.prettyPrint))) match {
      case Success(jsonData) => jsonData
      case Failure(e) => throw new RequestFailedException("The WBS timetabling service could not be reached", e)
    }
  }

  override def applyInternal(): Result = Result(
    syllabusPlusFeed = fetchSyllabusPlusFeed(),
    wbsFeed = fetchWbsFeed()
  )
}

trait TimetableCheckerCommandRequest {
  var warwickUniId: String = _
}

trait TimetableCheckerCommandPermissions extends RequiresPermissionsChecking {
  def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.Timetabling.Checker)
}

trait TimetableCheckerCommandValidation extends SelfValidating {
  self: TimetableCheckerCommandRequest
    with ProfileServiceComponent =>

  override def validate(errors: Errors): Unit =
    if (warwickUniId.isEmptyOrWhitespace || profileService.getMemberByUniversityIdStaleOrFresh(warwickUniId).isEmpty) {
      errors.rejectValue("warwickUniId", "NotEmpty.uniNumber")
    }
}
