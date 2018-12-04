package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime

trait CanBeStale {
	def missingFromImportSince: DateTime
}