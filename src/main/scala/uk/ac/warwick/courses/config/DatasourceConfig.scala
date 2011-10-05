package uk.ac.warwick.courses.config
import javax.sql.DataSource

abstract class DatasourceConfig {
	def dataSource:DataSource
}