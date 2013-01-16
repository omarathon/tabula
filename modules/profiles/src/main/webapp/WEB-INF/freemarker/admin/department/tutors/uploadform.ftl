<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<div id="tutor-upload-form">
	<h1>Upload personal tutors for ${department.name}</h1>
	
	<ul id="tutor-tabs" class="nav nav-tabs">
		<li class="active"><a href="#upload">Upload</a></li>
		<li><a href="#webform">Web Form</a></li>
	</ul>
	
	<div class="tab-content">
		<div class="tab-pane active" id="upload">
			<p>
				The tutor spreadsheet that you upload must be an .xlsx spreadsheet (created in Microsoft Office 2007+).
				The spreadsheet should have two columns in the following order: student ID then tutor ID.
				An optional third column may be added to contain tutor name in the case of external tutors.
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
		
		<div class="tab-pane" id="webform">
			<p>
				Click the add button below to update the personal tutor for a student.
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
	
			<@f.form id="tutor-webform" method="post" enctype="multipart/form-data" action="${url('/admin/department/${department.code}/tutors')}" commandName="uploadPersonalTutorsCommand">
				<input name="isfile" value="false" type="hidden"/>
				<table class="tutorTable">
					<tr>
						<th>Student ID</th>
						<th>Tutor ID</th>
						<th>Tutor Name <span class="muted">for non-University members</span></th>
					</tr>
					<#if tutorsToDisplay??>
						<#list tutorsToDisplay as item>
							<tr>
								<td>
									<div class="input-prepend">
										<span class="add-on"><i class="icon-user"></i></span>
										<input class="universityId span2" value="${item.universityId}" name="tutors[${item_index}].subjectUniversityId" type="text" readonly="readonly" />
									</div>
								</td>
								<td><input name="tutors[${item_index}].tutorUniversityId" value="<#if item.tutorUniversityId??>${item.tutorUniversityId}</#if>" type="text" /></td>
								<td><input name="tutors[${item_index}].tutorName" value="<#if item.tutorName??>${item.tutorName}</#if>" type="text" /></td>
							</tr>
						</#list>
					</#if>
				</table>
				
				<br />
				
				<button class="add-additional-tutors btn"><i class="icon-plus"></i> Add</button>
				<div class="submit-buttons">
					<input type="submit" class="btn btn-primary" value="Save">
					or <a href="<@routes.home />" class="btn">Cancel</a>
				</div>
			</@f.form>
		</div>
	</div>
</div>
</#escape>