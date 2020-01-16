package uk.ac.warwick.tabula.web.views

import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.regex.Pattern

import com.google.common.io.MoreFiles
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

/**
  * Checks Freemarker templates for syntax errors. This will include
  * things like an unclosed FTL tag or a local outside of a macro,
  * but NOT things like a typo in a variable name or even macro name.
  * Each template is parsed without any external input, and not executed.
  *
  * This class only parses files in common. It is subclassed in each web module
  * to check that module's files.
  */
class CommonFreemarkerSyntaxTest extends TestBase with Logging {
  @Test
  def parseAll(): Unit = {
    val resolver = new PathMatchingResourcePatternResolver
    val resources = resolver.getResources("/WEB-INF/freemarker/**/*.ftl")
    val conf = newFreemarkerConfiguration()
    if (resources.isEmpty) fail("No Freemarker templates found!")
    logger.debug(s"Checking syntax for ${resources.length} files")
    for (resource <- resources) {
      new freemarker.template.Template(resource.getFile.getAbsolutePath, new FileReader(resource.getFile), conf)
    }
  }

  // TAB-7321 Ensure that all FTL templates use <#escape x as x?html>
  @Test
  def xssSafety(): Unit = {
    val resolver = new PathMatchingResourcePatternResolver
    val resources = resolver.getResources("/WEB-INF/freemarker/**/*.ftl")
    if (resources.isEmpty) fail("No Freemarker templates found!")

    val whitelistedPaths = Seq(
      "emails/",
      "notifications/",
      "layouts/base-id-switch.ftl",
      "layouts/nonav-id-switch.ftl",
      "multiple_upload_help.ftl",
      "sysadmin/jobs/job-status-fragment.ftl",
      "cm2/admin/assignments/feedbackreport/job-status-fragment.ftl",
      "groups/admin/groups/import-spreadsheet/job-status-fragment.ftl",
      "cm2/submit/formfields/",
    )

    val badPaths: Seq[Path] = resources.toSeq.flatMap { resource =>
      // Walk back up to get the relative path
      val file = resource.getFile.toPath

      @tailrec
      def findTemplatesDir(p: Path): Path =
        if (p.getFileName.toString == "freemarker") p
        else findTemplatesDir(p.getParent)

      val templatesDir = findTemplatesDir(file)
      val relativePath = templatesDir.relativize(file)

      if (
        // Isn't on the whitelist
        !whitelistedPaths.exists(relativePath.toString.startsWith) &&

        // Is an ftl file
        MoreFiles.getFileExtension(file) == "ftl" &&

        // Assume files that start with _ are only included from elsewhere, therefore fine
        !MoreFiles.getNameWithoutExtension(file).startsWith("_")
      ) {
        val source = MoreFiles.asCharSource(file, StandardCharsets.UTF_8)
        lazy val content = source.read()
        lazy val strippedContent =
          content
            .replaceAll("(?s)<#ftl.*?>", "")
            .replaceAll("(?s)<#import.*?>", "")
            .replaceAll("(?s)<#macro.*?</#macro>", "")
            .replaceAll("(?s)<#function.*?</#function>", "")
            .replaceAll("<#assign [^=>]+=.+>", "")
            .replaceAll("(?s)<#assign.*?</#assign>", "")
            .replaceAll("</?#(if|elseif|else).*?>", "")
            .replaceAll("</?#compress[^>]*>", "")
            .replaceAll("(?s)<#--.*?-->", "")
            .trim

        val interpolation = Pattern.compile(".*\\$\\{(.*?)\\}.*")

        if (
          // Doesn't contain an <#escape x as x?html> line
          !source.lines().iterator().asScala.exists(_.matches(".*<#escape (\\S+) as \\1\\?html>.*")) &&

          // Interpolates a variable
          source.lines().iterator().asScala.exists { l =>
            val m = interpolation.matcher(l)
            var found: Boolean = false
            while (m.find() && !found) {
              // Allow inline ?html escaping
              if (!m.group(1).endsWith("?html")) found = true
            }
            found
          } &&

          // Has content that isn't just directives
          strippedContent.hasText
        ) {
          Some(relativePath)
        } else None
      } else None
    }

    if (badPaths.nonEmpty) {
      fail(s"The following ${badPaths.size} paths do not escape HTML:\n\n${badPaths.mkString("\n")}")
    }
  }
}
