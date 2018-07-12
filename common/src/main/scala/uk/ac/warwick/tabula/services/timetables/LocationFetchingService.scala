package uk.ac.warwick.tabula.services.timetables

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpUriRequest, RequestBuilder}
import org.apache.http.client.utils.HttpClientUtils
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Location, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent}
import uk.ac.warwick.util.cache.{Cache, CacheEntryFactory, Caches}
import uk.ac.warwick.util.web.Uri

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

/**
 * A service that takes a location as a String, and tries to turn it into a
 * MapLocation, if at all possible, else it will fall back to a NamedLocation.
 */
trait LocationFetchingService extends Logging {
	def mapLocationsFor(name: String): Try[Seq[WAI2GoLocation]]

	def allLocationsFor(name: String): Seq[Location] = mapLocationsFor(name) match {
		case Success(Nil) =>
			logger.info(s"No map locations returned for $name, returning NamedLocation")
			Seq(NamedLocation(name))

		case Success(locations) =>
			locations.map { loc => MapLocation(loc.name, loc.locationId) }

		case Failure(ex) =>
			logger.warn(s"Error requesting map location information for $name", ex)
			Nil
	}

	def locationFor(name: String): Location = allLocationsFor(name) match {
		case Seq(singleMatch) => singleMatch
		case _ => NamedLocation(name)
	}
}

class CachedLocationFetchingService(delegate: LocationFetchingService) extends LocationFetchingService with AutowiringCacheStrategyComponent {

	val CacheExpiryTime: Duration = 48.hours

	type CacheEntry = Try[Seq[WAI2GoLocation] with java.io.Serializable]

	val cacheEntryFactory: CacheEntryFactory[String, CacheEntry] = new CacheEntryFactory[String, CacheEntry] {
		def create(name: String): CacheEntry = delegate.mapLocationsFor(name).asInstanceOf[CacheEntry]

		def create(names: JList[String]): JMap[String, CacheEntry] = {
			JMap(names.asScala.map(name => (name, create(name))): _*)
		}

		def isSupportsMultiLookups = true
		def shouldBeCached(locations: CacheEntry): Boolean = locations.isSuccess // Don't cache failures
	}

	lazy val cache: Cache[String, CacheEntry] =
		Caches.newCache("WAI2GoLocationCache", cacheEntryFactory, CacheExpiryTime.toSeconds, cacheStrategy)

	override def mapLocationsFor(name: String): Try[Seq[WAI2GoLocation]] = cache.get(name)

}

trait WAI2GoConfiguration {
	def baseUri: Uri
	def cached: Boolean
	def defaultHeaders: Map[String, String]
	def defaultParameters: Map[String, String]
	def queryParameter: String
}

trait WAI2GoConfigurationComponent {
	def wai2GoConfiguration: WAI2GoConfiguration
}

trait AutowiringWAI2GoConfigurationComponent extends WAI2GoConfigurationComponent {
	val wai2GoConfiguration = new AutowiringWAI2GoConfiguration
	class AutowiringWAI2GoConfiguration extends WAI2GoConfiguration {
		val cached = true

		val projectId: String = "1"
		val clientId: String = "3"

		val baseUri: Uri = Uri.parse(s"https://campus-cms.warwick.ac.uk/api/v1/projects/$projectId/autocomplete.json")
		val defaultHeaders: Map[String, String] = Map(
			"Authorization" -> "Token 3a08c5091e5e477faa6ea90e4ae3e6c3",
			"Accept" -> "application/json"
		)
		val defaultParameters: Map[String, String] = Map(
			"client_id" -> clientId,
			"exact_limit" -> "2",
			"limit" -> "2"
		)
		val queryParameter = "term"
	}
}

trait LocationFetchingServiceComponent {
	def locationFetchingService: LocationFetchingService
}

trait WAI2GoHttpLocationFetchingServiceComponent extends LocationFetchingServiceComponent {
	self: WAI2GoConfigurationComponent =>

	lazy val locationFetchingService = WAI2GoHttpLocationFetchingService(wai2GoConfiguration)
}

private class WAI2GoHttpLocationFetchingService(config: WAI2GoConfiguration) extends LocationFetchingService {
	self: ApacheHttpClientComponent =>

	override def mapLocationsFor(name: String): Try[Seq[WAI2GoLocation]] = {
		logger.info(s"Requesting location info for $name")

		var response: CloseableHttpResponse = null

		try {
			response = httpClient.execute(requestForName(name))

			if (response.getStatusLine.getStatusCode == 200) {
				val responseBody = IOUtils.toString(response.getEntity.getContent)

				JSON.parseFull(responseBody) match {
					case Some(locations: Seq[Map[String, Any]] @unchecked) => Success(locations.flatMap(WAI2GoLocation.fromProperties))
					case _ => Success(Nil)
				}
			} else {
				throw new RuntimeException(s"Got unexpected response status ${response.getStatusLine.toString} from the WAI2Go location service")
			}
		} catch {
			case e: Exception => Failure(e)
		} finally {
			HttpClientUtils.closeQuietly(response)
		}
	}

	private def requestForName(name: String): HttpUriRequest = {
		val request =
			RequestBuilder.get(config.baseUri.toJavaUri)
				.addParameter(config.queryParameter, name)
		config.defaultParameters.foreach { case (n, v) => request.addParameter(n, v) }
		config.defaultHeaders.foreach { case (n, v) => request.addHeader(n, v) }

		request.build()
	}
}

object WAI2GoHttpLocationFetchingService {
	def apply(config: WAI2GoConfiguration): LocationFetchingService = {
		val service = new WAI2GoHttpLocationFetchingService(config) with AutowiringApacheHttpClientComponent

		if (config.cached) {
			new CachedLocationFetchingService(service)
		} else {
			service
		}
	}
}

case class WAI2GoLocation(name: String, building: String, floor: String, locationId: String)
object WAI2GoLocation {
	def fromProperties(properties: Map[String, Any]): Option[WAI2GoLocation] = {
		val name = properties.get("value").collect { case s: String => s }
		val locationId = properties.get("w2gid").collect { case x => x.toString }
		val building = properties.get("building").collect { case s: String => s }
		val floor = properties.get("floor").collect { case s: String => s }

		if (name.nonEmpty && locationId.nonEmpty) {
			Some(WAI2GoLocation(name.get, building.getOrElse(""), floor.getOrElse(""), locationId.get))
		} else {
			None
		}
	}
}