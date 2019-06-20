package uk.ac.warwick.tabula.system

import java.io.ByteArrayInputStream
import java.io.IOException

import org.xml.sax.{EntityResolver, InputSource, SAXException}

class SecureXmlEntityResolver extends EntityResolver {
  @throws[SAXException]
  @throws[IOException]
  def resolveEntity(publicId: String, systemId: String) = new InputSource(new ByteArrayInputStream(Array[Byte]()))
}
