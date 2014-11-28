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
				You can upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
				The spreadsheet should have three column headings in the following order: <b>ID, Mark, Grade</b>.
				You can use this <a href="<@routes.markermarkstemplate assignment=assignment  marker=marker/>" >generated spreadsheet</a> as a template.
				Note that you can upload just marks, just grades or both.
			</p>
			<@f.form method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/marker/${marker.warwickId}/marks')}" commandName="markerAddMarksCommand">
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
						<@f.errors path="file" cssClass="error" />
					</td>
				</tr>
			</table>
			<div class="submit-buttons">
				<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
			</div>
			</@f.form>
		</div>
		<div class="tab-pane " id="webform">
			<p>
				Click the add button below to enter marks for a student.
			</p>
			<table class="hide">
				<tbody class="row-markup">
				<tr class="mark-row">
					<td>
						<div class="input-prepend input-append">
							<span class="add-on"><i class="icon-user"></i></span>
							<input class="universityId span2" name="universityId" type="text" />
						</div>
					</td>
					<td><input name="actualMark" type="text" /></td>
					<td><input name="actualGrade" type="text" /></td>
				</tr>
				</tbody>
			</table>
			<@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/marker/${marker.warwickId}/marks')}" commandName="markerAddMarksCommand">
				<div class="fix-area">
					<input name="isfile" value="false" type="hidden"/>
					<table class="marksUploadTable">
						<tr class="mark-header"><th>University ID</th><th>Marks</th><th>Grade</th></tr>
							<#if marksToDisplay??>
								<#list marksToDisplay as markItem>
									<tr class="mark-row">
										<td>
											<div class="input-prepend input-append">
												<span class="add-on"><i class="icon-user"></i></span>
												<input class="universityId span2" value="${markItem.universityId}" name="marks[${markItem_index}].universityId" type="text" readonly="readonly" />
											</div>
										</td>
										<td><input name="marks[${markItem_index}].actualMark" value="<#if markItem.actualMark??>${markItem.actualMark}</#if>" type="text" /></td>
										<td><input name="marks[${markItem_index}].actualGrade" value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" /></td>
									</tr>
								</#list>
							</#if>
					</table>
					<br /><button class="add-additional-marks btn"><i class="icon-plus"></i> Add</button>
					<div class="submit-buttons fix-footer">
						<input type="submit" class="btn btn-primary" value="Save">
						or <a href="<@routes.listmarkersubmissions assignment marker/>" class="btn">Cancel</a>
					</div>
				</div>
			</@f.form>
		</div>
	</div>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	});
</script>
</#escape>
