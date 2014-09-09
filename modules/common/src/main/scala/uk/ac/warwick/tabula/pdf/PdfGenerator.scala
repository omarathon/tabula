package uk.ac.warwick.tabula.pdf

import com.itextpdf.text.Image
import org.apache.commons.io.IOUtils
import org.w3c.dom.Element
import org.xhtmlrenderer.extend.{ReplacedElement, UserAgentCallback, ReplacedElementFactory}
import org.xhtmlrenderer.layout.LayoutContext
import org.xhtmlrenderer.render.BlockBox
import org.xhtmlrenderer.simple.extend.FormSubmissionListener
import uk.ac.warwick.tabula.commands.profiles.ResizesPhoto
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.views.TextRendererComponent
import org.xhtmlrenderer.pdf.{ITextImageElement, ITextFSImage, ITextRenderer}
import java.io.OutputStream
import uk.ac.warwick.spring.Wire


trait PDFGeneratorComponent {
	def pdfGenerator: PdfGenerator

	trait PdfGenerator {
		def renderTemplate(templateId: String, model: Any,out:OutputStream)
	}

}

trait FreemarkerXHTMLPDFGeneratorComponent extends PDFGeneratorComponent {
	self: TextRendererComponent =>

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	def pdfGenerator: PdfGenerator = new PdfGeneratorImpl()

	class PdfGeneratorImpl extends PdfGenerator {
		def renderTemplate(templateId: String, model: Any, out:OutputStream) = {
			val xthml = textRenderer.renderTemplate(templateId, model)
			val renderer = new ITextRenderer
			renderer.getSharedContext.setReplacedElementFactory(new ProfileImageReplacedElementFactory(renderer.getSharedContext.getReplacedElementFactory))
			renderer.setDocumentFromString(xthml.replace("&#8194;", " "), topLevelUrl)
			renderer.layout()
			renderer.createPDF(out)
		}
	}

}

class ProfileImageReplacedElementFactory(delegate: ReplacedElementFactory) extends ReplacedElementFactory with ResizesPhoto with AutowiringProfileServiceComponent {

	size = THUMBNAIL_SIZE // TODO consider allowing different sizes

	override def createReplacedElement(c: LayoutContext, box: BlockBox, uac: UserAgentCallback, cssWidth: Int, cssHeight: Int) =
		Option(box.getElement).map { element =>
			Some(element)
				.filter { el => "img".equals(el.getNodeName) && el.hasAttribute("data-universityid") }
				.flatMap { el => profileService.getMemberByUniversityId(el.getAttribute("data-universityid"), true) }
				.flatMap { m =>
					val photo = render(Some(m))
					val image = Image.getInstance(IOUtils.toByteArray(photo.inputStream))
					Option(new ITextFSImage(image))
				}
				.map { fsImage =>
					if ((cssWidth != -1) || (cssHeight != -1)) fsImage.scale(cssWidth, cssHeight)
					new ITextImageElement(fsImage)
				}
				.getOrElse {
					delegate.createReplacedElement(c, box, uac, cssWidth, cssHeight)
				}
		}.orNull

	override def remove(e: Element) = delegate.remove(e)
	override def setFormSubmissionListener(listener: FormSubmissionListener) = delegate.setFormSubmissionListener(listener)
	override def reset() = delegate.reset()
}