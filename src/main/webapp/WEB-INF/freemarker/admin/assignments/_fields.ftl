<#-- 
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines. 
-->
<#macro datefield path label>
<@form.labelled_row path label>
<@f.input path=path cssClass="date-time-picker" />
</@form.labelled_row>
</#macro>

<@form.labelled_row "name" "Assignment name">
<@f.input path="name" cssClass="text" />
</@form.labelled_row>

<@datefield path="openDate" label="Open date" />
<@datefield path="closeDate" label="Close date" />

<#if newRecord>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	</@form.labelled_row>
	
<#else>

	<@form.labelled_row "academicYear" "Academic year">
	<@spring.bind path="academicYearString">
	<span class="uneditable-value">${status.value} <span class="hint">(can't be changed)<span></span>
	</@spring.bind>
	</@form.labelled_row>

</#if>

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

		<@form.row>
			<@form.label path="fileAttachmentLimit">Max attachments per submission</@form.label>
			<@form.field>
				<@f.select path="fileAttachmentLimit" cssClass="span1">
					<@f.options items=1..command.maxFileAttachments />
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
				<@f.textarea path="comment" id="assignmentComment" />
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

