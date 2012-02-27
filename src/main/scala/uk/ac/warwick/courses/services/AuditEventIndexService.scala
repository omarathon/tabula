package uk.ac.warwick.courses.services

import java.io.File
import java.io.FileNotFoundException
import scala.annotation.target.param
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field.Index
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.NumericField
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import uk.ac.warwick.courses.data.model.AuditEvent
import uk.ac.warwick.courses.helpers.Closeables.ensureClose
import org.apache.lucene.search.Query
import org.apache.lucene.analysis.SimpleAnalyzer
import org.apache.lucene.index.IndexNotFoundException
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.TopDocs
import uk.ac.warwick.userlookup.User

/**
 * Indexes audit events using Lucene.
 */
// Disabled until it's actually ready to use
//@Component 
class AuditEventIndexService() extends InitializingBean with QueryMethods {
	val LuceneVersion = Version.LUCENE_35
	
	@Autowired var service:AuditEventService =_ 
	@Value("${filesystem.index.audit.dir}") var indexPath:File =_
	@Value("${filesystem.create.missing}") var createMissingDirectories:Boolean =_
	var reader:IndexReader =_
	var searcher:IndexSearcher =_
	val analyzer = new StandardAnalyzer(LuceneVersion)
	val writerConfig = new IndexWriterConfig(LuceneVersion, analyzer)
	
	override def afterPropertiesSet {
		if (!indexPath.exists) {
			if (createMissingDirectories) indexPath.mkdirs 
			else throw new IllegalStateException("Audit event index path missing", new FileNotFoundException(indexPath.getAbsolutePath))
		}
		if (!indexPath.isDirectory) throw new IllegalStateException("Audit event index path not a directory: " + indexPath.getAbsolutePath)
		
		initialiseSearching
	}
	
	def initialiseSearching = 
		if (reader == null) {
			try {
				reader = IndexReader.open(FSDirectory.open(indexPath), true)
				searcher = new IndexSearcher(reader)
			} catch {
				case e:IndexNotFoundException => {
					reader = null
					searcher = null
				}
			}
		}
	
	def index = {
		val writer = new IndexWriter(FSDirectory.open(indexPath), writerConfig)
		// FIXME find the newest indexed item and use that date
		val latestIndexItem = new DateTime(1970,1,1,0,0)
		val newItems = service.listNewerThan(latestIndexItem, 1000)
		ensureClose(writer) { 
			for (item <- newItems if item.eventStage == "before")  
				writer.updateDocument(uniqueTerm(item), toDocument(item)) 
		}
		writer.close
	}
	
	def student(student:User) = search(
		new TermQuery(new Term("students", student.getWarwickId))
	)
	
	def f(usercode:String) = 
		search(new TermQuery(new Term("userId", usercode)))
	
	def search(query:Query) = {
		initialiseSearching
		val results = searcher.search(query, null, 200)
		transformResults(results)
	}
	
	def transformResults(results:TopDocs) = {
		val hits = results.scoreDocs
		hits.par.map{ hit =>
			searcher.doc(hit.doc)
		}
	}
	
	/**
	 * If an existing Document is in the index with this term, it
	 * will be replaced.
	 */
	def uniqueTerm(item:AuditEvent) = new Term("id", item.eventId)
		
	def toDocument(item:AuditEvent) : Document = {
		val doc = new Document
		doc add plainStringField("id", item.id)
		doc add plainStringField("eventId", item.eventId)
		doc add plainStringField("userId", item.userId) 
		if (item.masqueradeUserId != null) 
			doc add plainStringField("masqueradeUserId", item.masqueradeUserId)
		doc add plainStringField("eventType", item.eventType)
		
		service.parseData(item.data) match {
			case None => // no valid JSON
			case Some(data) => addDataToDoc(data, doc)
		}
		doc add dateField("eventDate", item.eventDate)		
		doc
	}
	
	private def addDataToDoc(data:Map[String,Any], doc:Document) = {
		for (key <- Seq("feedback", "assignment", "module", "department")) {
			data.get(key) match {
				case Some(value:String) => doc add plainStringField(key, value)
				case _ => // missing or not a string
			}
		}
		data("students") match {
			case studentIds:Array[String] =>
				val field = new Field("students", studentIds.mkString(" "), Store.NO, Index.ANALYZED_NO_NORMS)
				field.setIndexOptions(IndexOptions.DOCS_ONLY)
				doc add field
			case _ => 
		}
	}
	
	private def plainStringField(name:String, value:String) = {
		val field = new Field(name, value, Store.YES, Index.NOT_ANALYZED_NO_NORMS)
		field.setIndexOptions(IndexOptions.DOCS_ONLY)
		field
	}
	
	private def dateField(name:String, value:DateTime) = {
		val field = new NumericField(name)
		field.setLongValue(value.getMillis)
		field
	}
}

trait QueryMethods {
	private def boolean(occur:Occur, queries:Query*) = {
		val query = new BooleanQuery
		for (q <- queries) query.add(q, occur)
		query
	}
	
	def all(queries:Query*) = boolean(Occur.MUST, queries:_*)
	def some(queries:Query*) = boolean(Occur.SHOULD, queries:_*)
}