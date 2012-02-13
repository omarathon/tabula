package uk.ac.warwick.courses.services

import java.io.File
import java.io.FileNotFoundException
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import uk.ac.warwick.courses.helpers.Closeables._
import scala.annotation.target.field
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import collection.JavaConversions._
import org.apache.lucene.document.Document
import uk.ac.warwick.courses.data.model.AuditEvent

class AuditEventIndexer(val service:AuditEventService, val indexPath:File) {
	
	if (!indexPath.exists) throw new IllegalStateException("Audit event index path", new FileNotFoundException(indexPath.getAbsolutePath))
	if (!indexPath.isDirectory) throw new IllegalStateException("Audit event index path not a directory: " + indexPath.getAbsolutePath)
	
	val analyzer = new StandardAnalyzer(Version.LUCENE_35)
	val config = new IndexWriterConfig(Version.LUCENE_35, analyzer)
	
	def index {
		val writer = new IndexWriter(FSDirectory.open(indexPath), config)
		val latestIndexItem = new DateTime().withYear(1970)
		val newItems = service.listNewerThan(latestIndexItem, 1000)
		for (item <- newItems) writer.addDocument(toDocument(item))
		writer.close
	} 
	
	def toDocument(item:AuditEvent) : Document = {
		val doc = new Document
		
		doc
	}
	
}