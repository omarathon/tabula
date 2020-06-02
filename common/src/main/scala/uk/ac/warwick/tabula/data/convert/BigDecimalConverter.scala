package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter

import scala.util.Try

/**
  * For binding Scala BigDecimals
  */
class BigDecimalConverter extends TwoWayConverter[String, BigDecimal] {

  override def convertRight(asString: String): BigDecimal =
    Try(BigDecimal(asString).setScale(1, BigDecimal.RoundingMode.HALF_UP)).toOption.orNull

  override def convertLeft(bd: BigDecimal): String = Option(bd).map(_.toString).orNull

}
