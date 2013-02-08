<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
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
	
	<p>
		The tutor spreadsheet that you upload must be an <tt>.xlsx</tt> spreadsheet (created in Microsoft Office 2007+).
		The spreadsheet must have two columns headed: <tt>student_id</tt> and <tt>tutor_id</tt>.
		An optional <tt>tutor_name</tt> column may be added, but should <b>only</b> be set for external tutors
		who do not have a University number from Warwick. Tabula will ignore any other columns which you may set for your own reference.
		You can use this <a href="<@routes.tutor_template department=department />" >generated spreadsheet</a> as a template.
	</p>
	<@f.form method="post" enctype="multipart/form-data" action="${url('/admin/department/${department.code}/tutors')}" commandName="uploadPersonalTutorsCommand">
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
				</td>
			</tr>
		</table>

		<div class="submit-buttons">
			<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
		</div>
	</@f.form>
</div>
</#escape>