package uk.ac.warwick.tabula.helpers

import java.nio.charset.StandardCharsets
import java.util.{Base64, Date}

import io.jsonwebtoken.{Claims, Jws, Jwts, SignatureAlgorithm}
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
 * Helpers for using JSON web tokens.
 *
 * Adapted from JWTCookieDataCodec in Play Framework's Cookie class
 */
object JWT {
  // Use Joda now() so tests that call DateTimeUtils will work
  private val jwtClock = new io.jsonwebtoken.Clock {
    override def now(): Date = DateTime.now().toDate
  }

  /**
   * The JWT signature algorithm to use on the session cookie
   * uses 'alg' https://tools.ietf.org/html/rfc7515#section-4.1.1
   */
  private val signatureAlgorithm: String = "HS256"

  /**
   * The time after which the session is automatically invalided
   * Use 'exp' https://tools.ietf.org/html/rfc7519#section-4.1.4
   */
  private val expiresAfter: Option[FiniteDuration] = None

  /**
   * The amount of clock skew to accept between servers when performing date checks
   * If you have NTP or roughtime synchronizing between servers, you can enhance
   * security by tightening this value.
   */
  private val clockSkew: FiniteDuration = 5.minutes

  /**
   * Formats the input claims to a JWT string, and adds extra date related claims.
   *
   * @param secret the secret to encode values with
   * @param claims all the claims to be added to JWT.
   * @return the signed, encoded JWT with extra date related claims
   */
  def encode(secret: String)(claims: Map[String, AnyRef]): String = {
    val builder = Jwts.builder()
    val now     = jwtClock.now()

    // Add the claims one at a time because it saves problems with mutable maps
    // under the implementation...
    claims.foreach { case (k, v) => builder.claim(k, v) }

    // https://tools.ietf.org/html/rfc7519#section-4.1.4
    expiresAfter.map { duration =>
      val expirationDate = new Date(now.getTime + duration.toMillis)
      builder.setExpiration(expirationDate)
    }

    builder.setNotBefore(now) // https://tools.ietf.org/html/rfc7519#section-4.1.5
    builder.setIssuedAt(now)  // https://tools.ietf.org/html/rfc7519#section-4.1.6

    // Sign and compact into a string...
    val sigAlg = SignatureAlgorithm.valueOf(signatureAlgorithm)
    builder.signWith(sigAlg, Base64.getEncoder.encodeToString(secret.getBytes(StandardCharsets.UTF_8))).compact()
  }

  /**
   * Parses encoded JWT against configuration, returns all JWT claims.
   *
   * @param secret the secret to decode values with
   * @param encodedString the signed and encoded JWT.
   * @return the map of claims
   */
  def decode(secret: String)(encodedString: String): Map[String, AnyRef] = {
    val jws: Jws[Claims] = Jwts
      .parser()
      .setClock(jwtClock)
      .setSigningKey(Base64.getEncoder.encodeToString(secret.getBytes(StandardCharsets.UTF_8)))
      .setAllowedClockSkewSeconds(clockSkew.toSeconds)
      .parseClaimsJws(encodedString)

    val headerAlgorithm = jws.getHeader.getAlgorithm
    if (headerAlgorithm != signatureAlgorithm) {
      val id  = jws.getBody.getId
      val msg = s"Invalid header algorithm $headerAlgorithm in JWT $id"
      throw new IllegalStateException(msg)
    }

    jws.getBody.asScala.toMap
  }
}
