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
		
		<p>You can set personal tutors for many students at once by uploading an <samp title="Microsoft Office Open XML Format">.xlsx</samp> spreadsheet. <a href="#" class="use-popover" 
			data-title="Personal tutor spreadsheet"
			data-trigger="click"
			data-placement="bottom"
			data-html="true"
			data-content="&lt;p&gt;The spreadsheet must be in &lt;samp&gt;.xlsx&lt;/samp&gt; format (created in Microsoft Excel 2007 or newer, or another compatible spreadsheet application).&lt;p&gt;
&lt;p&gt;The spreadsheet must contain two columns:&lt;p&gt;
&lt;ul&gt;
&lt;li&gt;&lt;b&gt;student_id&lt;/b&gt; - contains the student's University ID number (also known as the library card number)&lt;/li&gt;
&lt;li&gt;&lt;b&gt;tutor_id&lt;/b&gt; - contains the tutor's University ID number&lt;/li&gt;
&lt;/ul&gt;
&lt;p&gt;You may need to &lt;a href='http://office.microsoft.com/en-gb/excel-help/format-numbers-as-text-HA102749016.aspx?CTT=1'&gt;format these columns&lt;/a&gt; as text to avoid Microsoft Excel removing 0s from the start of ID numbers.&lt;/p&gt;
&lt;p&gt;If there are students in your department with external tutors who do not have a University ID number, you may also add a tutor_name column containing the names of the external tutors. Leave this column blank for tutors who have an ID number.&lt;/p&gt;
&lt;p&gt;The spreadsheet may also contain other columns and information for your own reference (these will be ignored by Tabula).&lt;/p&gt;"><i class="icon-question-sign"></i></a></p>
		
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
						<@f.errors path="*" cssClass="error" />
					</td>
				</tr>
			</table>
	
			<div class="submit-buttons">
				<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
			</div>
		</@f.form>
	</div>
</#escape>