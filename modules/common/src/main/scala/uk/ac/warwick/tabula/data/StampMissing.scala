package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions.mutableSetAsJavaSet
import scala.collection.mutable.HashSet
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.Logging

trait StampMissing extends Daoisms with Logging {

	protected def stampMissingFromImport(
			seenIdentifiers: HashSet[String],
			importStart: DateTime,
			tableName: String,
			identifyingField: String) = {

		val numBatches = (seenIdentifiers.size / Daoisms.MaxInClauseCount) + 1

		var sqlString = "update " + tableName + " " + """
				set
					missingFromImportSince = :importStart
				where
			"""

		val notInClauses = seenIdentifiers.grouped(Daoisms.MaxInClauseCount).zipWithIndex.map {
				case (batch, index) => {
					s"$identifyingField not in (:identifierGroup$index)"
				}
		}
		sqlString = sqlString	+ notInClauses.mkString(" and ")

		logger.warn("sqlString: " + sqlString)

		var query = session.createQuery(sqlString)
			.setParameter("importStart", importStart)

		seenIdentifiers.grouped(Daoisms.MaxInClauseCount).zipWithIndex.foreach {
			case (batch, index) => query.setParameterList(s"identifierGroup$index", batch)
		}

		query.executeUpdate
	}
}

