<#escape x as x?html>

	<#assign formDestination><@routes.tutor_upload department /></#assign>

	<div id="tutor-upload-form">
		<h1>Upload personal tutors for ${department.name}</h1>

		<#if tutorCount??>
			<#if tutorCount?number == 0>
				<div class="alert alert-error">
					<a class="close" data-dismiss="alert">&times;</a>
					<p><i class="icon-warning-sign"></i> There were no valid personal tutors in the list you confirmed.</p>
				</div>
			<#else>
				<div class="alert alert-success">
					<a class="close" data-dismiss="alert">&times;</a>
					<p><i class="icon-ok"></i> <@fmt.p tutorCount?number "valid personal tutor" /> saved.</p>
				</div>
			</#if>
		</#if>

		<#assign introText>
			<p>The spreadsheet must be in <samp>.xlsx</samp> format (created in Microsoft Excel 2007 or newer, or another compatible spreadsheet application). You can download a template spreadsheet which is correctly formatted, ready for completion.<p>
			<p>The spreadsheet must contain two columns, headed:<p>
			<ul>
			<li><b>student_id</b> - contains the student's University ID number (also known as the library card number)</li>
			<li><b>tutor_id</b> - contains the tutor's University ID number</li>
			</ul>
			<p>You may need to <a href='http://office.microsoft.com/en-gb/excel-help/format-numbers-as-text-HA102749016.aspx?CTT=1'>format these columns</a> as text to avoid Microsoft Excel removing 0s from the start of ID numbers.</p>
			<p>If there are students in your department with external tutors who do not have a University ID number, you may also add a <b>tutor_name</b> column containing the names of the external tutors. Leave this column blank for tutors who have an ID number.</p>
			<p>The spreadsheet may also contain other columns and information for your own reference (these will be ignored by Tabula).</p>
		</#assign>

		<p>You can set personal tutors for many students at once by uploading a spreadsheet.
			<a href="#"
				id="tutor-intro"
				class="use-introductory<#if showIntro("tutor-intro")> auto</#if>"
				data-hash="${introHash("tutor-intro")}"
				data-title="Personal tutor spreadsheet"
				data-trigger="click"
				data-placement="bottom"
				data-html="true"
				data-content="${introText}"><i class="icon-question-sign"></i></a></p>

		<p><a href="<@routes.tutor_template department />" >Download a template spreadsheet</a></p>

		<@f.form method="post" enctype="multipart/form-data" action=formDestination commandName="command">
			<input name="isfile" value="true" type="hidden"/>
			<table role="presentation" class="narrowed-form">
				<tr>
					<td id="multifile-column">
						<h3>Select file</h3>
						<p id="multifile-column-description">
							<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
						</p>
						<@form.labelled_row "file.upload" "Files">
							<input type="file" name="file.upload" multiple />
						</@form.labelled_row>
						<#if showErrors??>
							<@f.errors path="*" cssClass="error" />
						</#if>
					</td>
				</tr>
			</table>

			<div class="submit-buttons">
				<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
			</div>
		</@f.form>
	</div>
</#escape>