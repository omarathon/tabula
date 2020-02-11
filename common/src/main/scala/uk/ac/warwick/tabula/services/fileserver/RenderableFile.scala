package uk.ac.warwick.tabula.services.fileserver

import com.google.common.io.ByteSource

trait RenderableFile {
  def byteSource: ByteSource

  def filename: String

  def contentType: String

  def contentLength: Option[Long]

  def suggestedFilename: Option[String] = None

  def cachePolicy: CachePolicy = CachePolicy()

  def contentDisposition: ContentDisposition = ContentDisposition.Default

  def withSuggestedFilename(name: String): RenderableFile = new RenderableFile {
    override def byteSource: ByteSource = RenderableFile.this.byteSource
    override def filename: String = RenderableFile.this.filename
    override def contentType: String = RenderableFile.this.contentType
    override def contentLength: Option[Long] = RenderableFile.this.contentLength
    override def suggestedFilename: Option[String] = Some(name)
    override def cachePolicy: CachePolicy = RenderableFile.this.cachePolicy
    override def contentDisposition: ContentDisposition = RenderableFile.this.contentDisposition
  }

  def withContentDisposition(newContentDisposition: ContentDisposition): RenderableFile = new RenderableFile {
    override def byteSource: ByteSource = RenderableFile.this.byteSource
    override def filename: String = RenderableFile.this.filename
    override def contentType: String = RenderableFile.this.contentType
    override def contentLength: Option[Long] = RenderableFile.this.contentLength
    override def suggestedFilename: Option[String] = RenderableFile.this.suggestedFilename
    override def cachePolicy: CachePolicy = RenderableFile.this.cachePolicy
    override def contentDisposition: ContentDisposition = newContentDisposition
  }
}
