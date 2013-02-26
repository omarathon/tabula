package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty

import org.springframework.util.StringUtils

import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString

sealed abstract class AddressType(val dbValue: String)

object AddressType {
	case object Home extends AddressType("H")
	case object TermTime extends AddressType("C")

	def fromCode(code: String) = code match {
	  	case Home.dbValue => Home
	  	case TermTime.dbValue => TermTime
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

@Entity
class Address extends GeneratedId with ToString {
	@BeanProperty var line1: String = _
	@BeanProperty var line2: String = _
	@BeanProperty var line3: String = _
	@BeanProperty var line4: String = _
	@BeanProperty var line5: String = _
	@BeanProperty var postcode: String = _
	@BeanProperty var telephone: String = _
	
	@transient @BeanProperty var addressType: AddressType = null
	
	def isEmpty = {
		!(StringUtils.hasText(line1) || StringUtils.hasText(line2) || StringUtils.hasText(line3) ||
		StringUtils.hasText(line4) || StringUtils.hasText(line5) || StringUtils.hasText(postcode) ||
		StringUtils.hasText(telephone))
	}
		
	def toStringProps = Seq(
		"line1" -> line1,
		"line2" -> line2,
		"line3" -> line3,
		"line4" -> line4,
		"line5" -> line5,
		"postcode" -> postcode)

}