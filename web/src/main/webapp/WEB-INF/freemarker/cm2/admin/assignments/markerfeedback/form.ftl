<#escape x as x?html>
	<#import "*/cm2_macros.ftl" as cm2 />
	<@cm2.assignmentHeader "Submit feedback" assignment "for" />
	<div id="batch-feedback-form">
		<#assign submitUrl><@routes.cm2.uploadmarkerfeedback assignment marker /></#assign>
		<@f.form method="post" enctype="multipart/form-data" action=submitUrl commandName="addMarkerFeedbackCommand">
			<input type="hidden" name="batch" value="true">
			<p>The feedback filenames need to contain the student's University ID
				(e.g. <span class="example-filename">0123456 feedback.doc</span>) in order to match each
				file with the correct student. If you are using a Zip file, you could also put each file inside
				a folder whose name includes the University ID
				(e.g. <span class="example-filename">0123456/feedback.doc</span>).
			</p>
			<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
				<p>
					You can <a  href="<@routes.cm2.markerTemplatesZip assignment />">download a zip</a>
					containing copies of this assignments feedback template for each student with the university IDs added for you.
				</p>
			</#if>
			<div class="row">
				<div class="col-md-9">
					<h3>Select files</h3>
					<p id="multifile-column-description">
						<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
					</p>
					<@bs3form.labelled_form_group path="file.upload" labelText="Files">
						<input type="file" name="file.upload" multiple/>
					</@bs3form.labelled_form_group>
				</div>
				<div class="col-md-3">
					<h3>&hellip;or use a Zip file <img src="/static/images/icons/package-x-generic.png" alt="" style="margin-bottom: -8px"></h3>
					<p>You can pack the files into a Zip file if you prefer, or if your browser doesn't support uploading multiple files at once.</p>
					<@bs3form.labelled_form_group path="archive" labelText="Zip archive">
						<input type="file" name="archive"/>
					</@bs3form.labelled_form_group>
				</div>
			</div>
			<div class="submit-buttons">
				<button class="btn btn-primary btn-large spinnable spinner-auto" type="submit" name="submit-confirm" data-loading-text="Loading&hellip;">
					Upload
				</button>
			</div>
		</@f.form>
	</div>
</#escape>