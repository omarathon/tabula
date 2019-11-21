package uk.ac.warwick.tabula.system

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.stereotype.Component
import org.springframework.web.servlet.FlashMap
import org.springframework.web.servlet.support.AbstractFlashMapManager
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.JWT
import uk.ac.warwick.tabula.system.CookieFlashMapManager._
import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.tabula.web.Cookies._

import scala.jdk.CollectionConverters._

/**
 * Stores flash attributes in a cookie, based on Play's implementation. Flash values can *only*
 * contain simple `String` values, so this will throw an exception if it tries to serialize anything
 * that isn't a simple `String`.
 */
@Component
class CookieFlashMapManager extends AbstractFlashMapManager {
  private val secret: String = Wire.property("${cookies.flash.secret}")

  private def serialize(flashMaps: Seq[FlashMap]): Seq[Map[String, String]] =
    flashMaps.map { flashMap =>
      flashMap.put(TargetRequestPathAttribute, flashMap.getTargetRequestPath)
      flashMap.put(ExpirationTimeAttribute, flashMap.getExpirationTime)
      flashMap.asScala.view.mapValues(_.toString).toMap
    }

  private def deserialize(data: Seq[Map[String, String]]): Seq[FlashMap] =
    data.map { d =>
      val flashMap = new FlashMap
      flashMap.setTargetRequestPath(d(TargetRequestPathAttribute))
      flashMap.setExpirationTime(d(ExpirationTimeAttribute).toLong)
      flashMap.putAll(d.removedAll(Seq(TargetRequestPathAttribute, ExpirationTimeAttribute)).asJava)
      flashMap
    }

  private def encode(data: Seq[Map[String, String]]): String =
    JWT.encode(secret)(Map("data" -> data.map(_.asJava).asJava))

  private def decode(encoded: String): Seq[Map[String, String]] =
    JWT.decode(secret)(encoded)("data").asInstanceOf[JList[JMap[String, AnyRef]]]
      .asScala.toSeq
      .map(_.asScala.view.mapValues(_.toString).toMap)

  override def retrieveFlashMaps(request: HttpServletRequest): JList[FlashMap] =
    request.getCookies.getString(CookieName).map { encoded =>
      JArrayList(deserialize(decode(encoded)))
    }.getOrElse(JArrayList())

  override def updateFlashMaps(flashMaps: JList[FlashMap], request: HttpServletRequest, response: HttpServletResponse): Unit = {
    def valuesAreAllStrings(flashMap: FlashMap): Boolean =
      flashMap.asScala.values.forall(_.isInstanceOf[String])

    def specialAttributesNotSet(flashMap: FlashMap): Boolean =
      !flashMap.containsKey(TargetRequestPathAttribute) && !flashMap.containsKey(ExpirationTimeAttribute)

    require(flashMaps.asScala.forall(valuesAreAllStrings), "CookieFlashMapManager only allows flash values that are Strings")
    require(flashMaps.asScala.forall(specialAttributesNotSet), s"FlashMaps should not set special attributes $TargetRequestPathAttribute or $ExpirationTimeAttribute")

    val nonEmptyFlashMaps = flashMaps.asScala.filterNot { f => f.isEmpty || f.isExpired }.toSeq
    if (nonEmptyFlashMaps.isEmpty) {
      // Discard cookie
      response.addCookie(new Cookie(
        name = CookieName,
        value = "",
        maxAge = Some(0)
      ))
    } else if (nonEmptyFlashMaps.nonEmpty) {
      // Set cookie
      response.addCookie(new Cookie(
        name = CookieName,
        value = encode(serialize(nonEmptyFlashMaps)),
      ))
    }
  }
}

object CookieFlashMapManager {
  val CookieName = "Flash-Tabula"

  val TargetRequestPathAttribute = "__targetRequestPath"
  val ExpirationTimeAttribute = "__expirationTime"
}
