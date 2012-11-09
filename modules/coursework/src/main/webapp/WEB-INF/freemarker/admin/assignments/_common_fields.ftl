<#-- 

This section contains the form fields that can apply to a group of
assignments, as well as to an individual one.

If you add a field it should also be added to _common_fields_hidden.ftl
so that they can be passed around between requests.

-->

<#if features.submissions>
	<@form.labelled_row "collectSubmissions" "Submissions">
		<label class="checkbox">
			<@f.checkbox path="collectSubmissions" id="collectSubmissions" />
			Collect submissions
		</label>
	</@form.labelled_row>
	<fieldset id="submission-options">
		<legend>Submission options</legend>

		<#if features.collectMarks>
			<@form.row>
				<@form.label></@form.label>
				<@form.field>
					<label class="checkbox">
						<@f.checkbox path="collectMarks" />
						Collect marks
					</label>
				</@form.field>
			</@form.row>
		</#if>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="displayPlagiarismNotice" />
					Show plagiarism notice
				</label>
			</@form.field>
		</@form.row>

		<#if features.assignmentMembership>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="restrictSubmissions" />
					Only allow enrolled students to submit
				</label>
				<div class="help-block">
					If you use this option, only students defined above as members will be able to
					submit, so make sure that the membership is correct to avoid problems.
				</div>
			</@form.field>
		</#if>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="allowLateSubmissions" />
					Allow submissions after the close date
				</label>
			</@form.field>
		</@form.row>
		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="allowResubmission" />
					Allow students to re-submit work
				</label>
				<div class="help-block">
					Students will be able to submit new work, replacing any previous submission.
					Re-submission is only allowed before the close date.
				</div>
			</@form.field>
		</@form.row>

		<#if features.extensions>
			<@form.row>
				<@form.label></@form.label>
				<@form.field>
					<label class="checkbox">
						<@f.checkbox path="allowExtensions" id="allowExtensions" />
						Allow extensions
					</label>
				</@form.field>
			</@form.row>
			<div id="request-extension-row">
				<@form.row>
					<@form.label></@form.label>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="allowExtensionRequests" />
							Allow students to request extensions
						</label>
						<div class="help-block">
							Students will be able to request extensions for this assignment via the submission page.
						</div>
					</@form.field>
				</@form.row>
			</div>
		</#if>

		<@form.row>
			<@form.label path="fileAttachmentLimit">Max attachments per submission</@form.label>
			<@form.field>
				<@spring.bind path="maxFileAttachments">
					<#assign maxFileAttachments=status.actualValue />
				</@spring.bind>
				<@f.select path="fileAttachmentLimit" cssClass="span1">
					<@f.options items=1..maxFileAttachments />
				</@f.select>
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label path="fileAttachmentTypes">Accepted attachment file types</@form.label>
			<@form.field>
				<@f.errors path="fileAttachmentTypes" cssClass="error" />
				<@f.input path="fileAttachmentTypes"  type="hidden" />
				<script type="text/javascript" src="/static/js/textList.js"></script>
				<script type="text/javascript">
					jQuery(document).ready(function(){
						var textListController = new TextListController('#fileExtensionList', '#fileAttachmentTypes');
						textListController.transformInput = function(text){
							var result = text.replace(new RegExp('\\.', 'g') , '');
							return result.toLowerCase();
						};
						textListController.preventDuplicates = true;
						textListController.init();
					});
				</script>
				<div id="fileExtensionList" class="textBoxListContainer">
					<ul>
						<li class="inputContainer"><input class="text" type="text"></li>
					</ul>
				</div>
				<div class="help-block">
					Enter the file types you would like to allow separated by spaces (e.g. "pdf doc docx"). Only attachments with the extensions specified will be permitted. Leave this field blank to accept attachments with any extension.
				</div>
			</@form.field>
		</@form.row>

		<div>
		<@form.row path="comment">
		  <@form.label for="assignmentComment">Text to show on submission form:</@form.label>
		  	<@form.field>
				<@f.errors path="comment" cssClass="error" />
				<@f.textarea path="comment" id="assignmentComment" rows="6" cssClass="span6" />
				<div class="help-block">
					You can make a new paragraph by leaving a blank line (i.e. press Enter twice).
				</div>
			</@form.field>
		</@form.row>
	</fieldset>


<#--
	<@form.row>
	<@form.field>



	</@form.field>
	</@form.row>
-->

</#if>