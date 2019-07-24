package uk.ac.warwick.tabula.services

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.CSRFFilter
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.queue.conversion.ItemType

import scala.beans.BeanProperty

trait CsrfService {
  def enforce: Boolean

  def enforce_=(e: Boolean): Unit
}

@Service
class CsrfServiceImpl(filter: CSRFFilter) extends CsrfService with Logging {
  @Value("${csrf.enforce}") var _enforce: Boolean = _

  def enforce: Boolean = _enforce

  def enforce_=(e: Boolean): Unit = {
    logger.info(s"Setting CSRF enforce to {}", e)
    filter.setReportOnlyMode(!e)
    _enforce = e
  }
}

@ItemType("CsrfEnforce")
@JsonAutoDetect
class CsrfEnforceMessage {
  def this(enforce: Boolean) {
    this()

    this.enforce = enforce
  }

  @BeanProperty var enforce: Boolean = _
}

trait CsrfServiceComponent {
  val csrfService: CsrfService
}

trait AutowiringCsrfServiceComponent extends CsrfServiceComponent {
  override val csrfService: CsrfService = Wire[CsrfService]
}

