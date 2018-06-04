package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.MarkPoint
import uk.ac.warwick.tabula.system.TwoWayConverter

import scala.util.Try

class MarkPointConverter extends TwoWayConverter[String, MarkPoint] {
	override def convertRight(source: String): MarkPoint = Try(source.toInt).map(MarkPoint.forMark).toOption.orNull

	override def convertLeft(source: MarkPoint): String = source.mark.toString
}
