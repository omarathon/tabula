package uk.ac.warwick.tabula.pdf

import uk.ac.warwick.tabula.web.views.{TextRendererComponent, TextRenderer}
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.{OutputStream, File, FileOutputStream, ByteArrayOutputStream}


trait PDFGeneratorComponent {
	def pdfGenerator: PdfGenerator

	trait PdfGenerator {
		def renderTemplate(templateId: String, model: Any,out:OutputStream)
	}

}

trait FreemarkerXHTMLPDFGeneratorComponent extends PDFGeneratorComponent {
	this: TextRendererComponent =>

	def pdfGenerator: PdfGenerator = new PdfGeneratorImpl()

	class PdfGeneratorImpl extends PdfGenerator{

		def renderTemplate(templateId: String, model: Any, out:OutputStream) = {

			val xthml = textRenderer.renderTemplate(templateId, model)
			val renderer = new ITextRenderer()
			renderer.setDocumentFromString(xthml)
			renderer.layout()
			renderer.createPDF(out)
		}
	}

}
