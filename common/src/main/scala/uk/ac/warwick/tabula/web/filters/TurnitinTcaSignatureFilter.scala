package uk.ac.warwick.tabula.web.filters

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader}
import java.security.MessageDigest
import java.util.Base64

import javax.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader

import javax.servlet.ServletInputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.{FilterChain, ReadListener}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.turnitintca.AutowiringTurnitinTcaConfigurationComponent
import uk.ac.warwick.tabula.web.filters.TurnitinTcaSignatureFilter.MultipleReadHttpRequest
import uk.ac.warwick.util.web.filter.AbstractHttpFilter

class TurnitinTcaSignatureFilter extends AbstractHttpFilter with Logging with AutowiringFeaturesComponent with AutowiringTurnitinTcaConfigurationComponent {

  lazy val keySpec = new SecretKeySpec(Base64.getDecoder.decode(tcaConfiguration.signingSecret), "HmacSHA256")

  override def doFilter(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain): Unit = {
    val requestWrapper = new MultipleReadHttpRequest(req)

    val body = IOUtils.toByteArray(requestWrapper.getInputStream)
    val signature = req.getHeader("X-Turnitin-Signature")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(keySpec)
    val hmac = mac.doFinal(body).map("%02X" format _).mkString.toLowerCase()

    if (signature != null && MessageDigest.isEqual(hmac.getBytes, signature.getBytes)) {
      chain.doFilter(requestWrapper, res)
    } else {
      logger.error(s"X-Turnitin-Signature is invalid ${new String(body, "UTF-8")}")
      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-Turnitin-Signature is invalid")
    }
  }
}

object TurnitinTcaSignatureFilter {
  class MultipleReadHttpRequest(val request: HttpServletRequest) extends HttpServletRequestWrapper(request) {

    private var cachedBytes: ByteArrayOutputStream = _

    override def getInputStream: ServletInputStream = {
      if (cachedBytes == null) cacheInputStream()
      new CachedServletInputStream()
    }

    override def getReader: BufferedReader = {
      new BufferedReader(new InputStreamReader(getInputStream))
    }

    private def cacheInputStream(): Unit = {
      cachedBytes = new ByteArrayOutputStream()
      IOUtils.copy(super.getInputStream, cachedBytes)
    }

    class CachedServletInputStream() extends ServletInputStream {
      private val input = new ByteArrayInputStream(cachedBytes.toByteArray)
      override def read: Int = input.read

      override def isFinished: Boolean = throw new UnsupportedOperationException
      override def isReady: Boolean = throw new UnsupportedOperationException
      override def setReadListener(readListener: ReadListener): Unit = throw new UnsupportedOperationException
    }
  }
}




