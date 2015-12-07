package uk.ac.warwick.tabula.pdf

import java.net.URL

import com.itextpdf.text.Image
import org.w3c.dom.Element
import org.xhtmlrenderer.extend.{UserAgentCallback, ReplacedElementFactory}
import org.xhtmlrenderer.layout.LayoutContext
import org.xhtmlrenderer.render.BlockBox
import org.xhtmlrenderer.simple.extend.FormSubmissionListener
import uk.ac.warwick.tabula.commands.profiles.{MemberPhotoUrlGeneratorComponent, ServesPhotosFromExternalApplication, PhotosWarwickMemberPhotoUrlGeneratorComponent}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.views.TextRendererComponent
import org.xhtmlrenderer.pdf.{ITextImageElement, ITextFSImage, ITextRenderer}
import java.io.OutputStream
import uk.ac.warwick.spring.Wire

trait PdfGenerator {
	def renderTemplate(templateId: String, model: Any,out:OutputStream)
}

trait PDFGeneratorComponent {
	def pdfGenerator: PdfGenerator
}

trait FreemarkerXHTMLPDFGeneratorComponent extends PDFGeneratorComponent {
	self: TextRendererComponent with MemberPhotoUrlGeneratorComponent =>

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	def pdfGenerator: PdfGenerator = new PdfGeneratorImpl()
	val parentUrlGenerator = photoUrlGenerator

	class PdfGeneratorImpl extends PdfGenerator {

		def renderTemplate(templateId: String, model: Any, out:OutputStream) = {
			val xthml = textRenderer.renderTemplate(templateId, model)
			val renderer = new ITextRenderer
			val elementFactory = new ProfileImageReplacedElementFactory(renderer.getSharedContext.getReplacedElementFactory)
				with MemberPhotoUrlGeneratorComponent
			renderer.getSharedContext.setReplacedElementFactory(elementFactory)
			renderer.setDocumentFromString(xthml.replace("&#8194;", " "), topLevelUrl)
			renderer.layout()
			renderer.createPDF(out)
		}
	}

}

class ProfileImageReplacedElementFactory(delegate: ReplacedElementFactory) extends ReplacedElementFactory
	with ServesPhotosFromExternalApplication with PhotosWarwickMemberPhotoUrlGeneratorComponent with AutowiringProfileServiceComponent {

	this: MemberPhotoUrlGeneratorComponent =>

	size = THUMBNAIL_SIZE // TODO consider allowing different sizes

	override def createReplacedElement(c: LayoutContext, box: BlockBox, uac: UserAgentCallback, cssWidth: Int, cssHeight: Int) = {
		Option(box.getElement).map { element =>
			Some(element)
				.filter { el => "img".equals(el.getNodeName) && el.hasAttribute("data-universityid") }
				.flatMap { el => profileService.getMemberByUniversityId(el.getAttribute("data-universityid"), disableFilter=true) }
				.flatMap { m =>
					val url = new URL(photoUrl(Option(m)))
					val image = Image.getInstance(url)
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
	}

	override def remove(e: Element) = delegate.remove(e)
	override def setFormSubmissionListener(listener: FormSubmissionListener) = delegate.setFormSubmissionListener(listener)
	override def reset() = delegate.reset()
}