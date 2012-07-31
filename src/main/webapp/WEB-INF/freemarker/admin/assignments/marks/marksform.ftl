<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="batch-feedback-form">	
	<h1>Submit marks for ${assignment.name}</h1>

	<ul id="marks-tabs" class="nav nav-tabs">
	    <li class="active"><a href="#upload">Upload</a></li>
	    <li><a href="#webform">Web Form</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="upload">
			<p>
				The marks spreadsheet that you upload must be an .xlsx spreadsheet (created in Microsoft Office 2007+).
				The spreadsheet should have three columns in the following order: student ID, mark, grade.
				You can use this <a href="<@url resource="/static/files/example.xlsx"/>" >generated spreadsheet</a> as a template.
			</p>
			<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/marks" commandName="addMarksCommand">
				<table role="presentation" class="narrowed-form">
					<tr>
						<td id="multifile-column">
							<h3>Select file</h3>
							<p id="multifile-column-description">
								Your browser doesn't seem able to handle uploading multiple files<noscript>
								(or it does, but your browser is not running the Javascript needed to support it)
								</noscript>.
								A recent browser like Google Chrome or Firefox will be able to upload multiple files.
								You can still upload a single marks spreadsheet file here if you want. 
								<div id="multifile-column-description-enabled" style="display:none">
								This uploader allows you to upload multiple marks spreadsheets at once. They
								will need to be in the same folder on your computer for you to be
								able to select them all.
								</div>
							</p>
							<@form.labelled_row "file.upload" "Files">
								<input type="file" name="file.upload" multiple />
							</@form.labelled_row>
						</td>
					</tr>
				</table>
				<script>
					if (Supports.multipleFiles) {
					  	jQuery('#multifile-column')
					  		.find('h3').html('Select files');
						jQuery('#multifile-column-description')
					  		.html(jQuery('#multifile-column-description-enabled').html());
					}
				</script>
				<div class="submit-buttons">
					<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
				</div>
			</@f.form>
		</div>
		<div class="tab-pane " id="webform">
			<p>
				Click the add button below to enter marks for a student.
			</p>
			<@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/marks" commandName="addMarksCommand">
				<table class="marksUploadTable">
					<tr class="mark-header"><th>University ID</th><th>Marks</th><th>Grade</th></tr>
					<#-- leave this stuff out until we can specify enrolled students
					<#if assignment.members??>
						<#list assignment.members.members as member>
							<tr class="mark-row">
								<td>${member}<input type="hidden" name="marks[member_index].universityId" value="${member}" /></td>
								<td><input type="text" name="marks[member_index].actualMark" value="" /></td>
								<td><input type="text" name="marks[member_index].actualGrade" value="" /></td>
							</tr>
						</#list>
					</#if>
					-->
				</table>
				<br /><button class="add-additional-marks btn"><i class="icon-plus"></i> Add</button>
				<div class="submit-buttons">
					<input type="submit" class="btn btn-primary" value="Save">
					or <a href="<@routes.depthome module=assignment.module />" class="btn">Cancel</a>
				</div>
			</@f.form>
		</div>
	</div>
</div>
</#escape>