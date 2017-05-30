<#escape x as x?html>
		<div class="deptheader">
			<h1>Submit feedback</h1>
			<h4 class="with-related"><span class="muted">for</span> ${assignment.name}</h4>
		</div>
		<div id="batch-feedback-form">
			<#assign submitUrl><@routes.cm2.addFeedback assignment /></#assign>
			<@f.form method="post" enctype="multipart/form-data" action=submitUrl commandName="addFeedbackCommand">
				<input type="hidden" name="batch" value="true">
				<p>The feedback filenames need to contain the student's University ID
					(e.g. <span class="example-filename">0123456 feedback.doc</span>) in order to match each
					file with the correct student. If you are using a Zip file, you could also put each file inside
					a folder whose name includes the University ID
					(e.g. <span class="example-filename">0123456/feedback.doc</span>).
				</p>
				<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
					<p>
						You can <a  href="<@routes.cm2.downloadFeedbackTemplates assignment />">download a zip</a>
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
					<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
				</div>
		</@f.form>
	</div>
</#escape>