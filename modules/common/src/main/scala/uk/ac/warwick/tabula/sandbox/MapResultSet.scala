package uk.ac.warwick.tabula.sandbox

import java.math.BigDecimal

import org.joda.time.DateTime
import uk.ac.warwick.util.core.sql.{AbstractResultSet, AbstractResultSetMetaData}

class MapResultSet(map: Map[String, _]) extends AbstractResultSet {

	override def getString(columnLabel: String) = getObject(columnLabel, classOf[String])
	override def getInt(columnLabel: String) = getObject(columnLabel, classOf[Int])
	override def getDate(columnLabel: String) = new java.sql.Date(getObject(columnLabel, classOf[DateTime]).getMillis)
	override def getBigDecimal(columnLabel: String) = getObject(columnLabel, classOf[BigDecimal])

	override def getObject[A](columnLabel: String, objectType: Class[A]) = map(columnLabel).asInstanceOf[A]

	override def getMetaData = {
		val columns = map.keys.toSeq

		new AbstractResultSetMetaData {
			override def getColumnCount = columns.size
			override def getColumnName(index: Int) = columns(index - 1)
		}
	}

}