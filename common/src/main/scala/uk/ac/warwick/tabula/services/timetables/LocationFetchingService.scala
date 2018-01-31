package uk.ac.warwick.tabula.services.timetables

import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.client.utils.{HttpClientUtils, URIBuilder}
import org.apache.http.util.EntityUtils
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Location, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent}
import uk.ac.warwick.util.cache.{Cache, CacheEntryFactory, Caches}
import uk.ac.warwick.util.web.Uri

import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

/**
 * A service that takes a location as a String, and tries to turn it into a
 * MapLocation, if at all possible, else it will fall back to a NamedLocation.
 */
trait LocationFetchingService {
	def locationFor(name: String): Location
}

class CachedLocationFetchingService(delegate: LocationFetchingService) extends LocationFetchingService with AutowiringCacheStrategyComponent {

	val CacheExpiryTime: Int = 60 * 60 * 48 // 48 hours in seconds

	val cacheEntryFactory = new CacheEntryFactory[String, Location] {
		def create(name: String): Location = delegate.locationFor(name)

		def create(names: JList[String]): JMap[String, Location] = {
			JMap(names.asScala.map(name => (name, create(name))): _*)
		}

		def isSupportsMultiLookups = true
		def shouldBeCached(location: Location) = true
	}

	lazy val cache: Cache[String, Location] =
		Caches.newCache("LocationCache", cacheEntryFactory, CacheExpiryTime, cacheStrategy)

	def locationFor(name: String): Location = cache.get(name)

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

private class WAI2GoHttpLocationFetchingService(config: WAI2GoConfiguration) extends LocationFetchingService with Logging {
	self: ApacheHttpClientComponent =>

	def locationFor(name: String): Location = {
		logger.info(s"Requesting location info for $name")

		val uriBuilder = new URIBuilder(config.baseUri.toJavaUri)
		uriBuilder.addParameter(config.queryParameter, name)
		config.defaultParameters.foreach { case (n, v) => uriBuilder.addParameter(n, v) }

		val get = new HttpGet(uriBuilder.build())
		config.defaultHeaders.foreach { case (n, v) => get.addHeader(n, v) }

		var response: CloseableHttpResponse = null
		var entity: HttpEntity = null

		val result: Try[Seq[WAI2GoLocation]] = try {
			response = httpClient.execute(get)

			if (response.getStatusLine.getStatusCode == 200) {
				entity = response.getEntity
				val json = IOUtils.toString(entity.getContent)

				JSON.parseFull(json) match {
					case Some(locations: Seq[Map[String, Any]]@unchecked) => Success(locations.flatMap(WAI2GoLocation.fromProperties))
					case _ => Success(Nil)
				}
			} else {
				throw new RuntimeException(s"Got unexpected response status ${response.getStatusLine.toString} from the WAI2Go location service")
			}
		} catch {
			case e: Exception => Failure(e)
		} finally {
			EntityUtils.consumeQuietly(entity)
			HttpClientUtils.closeQuietly(response)
		}

		result match {
			case Success(locations)
				if locations.size == 1 =>
					MapLocation(locations.head.name, locations.head.locationId)

			case Success(locations) =>
				logger.info(s"Multiple locations (or no locations) returned for $name, returning NamedLocation")
				NamedLocation(name)

			case Failure(ex) =>
				logger.warn(s"Error requesting location information for $name, returning NamedLocation", ex)
				NamedLocation(name)
		}
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