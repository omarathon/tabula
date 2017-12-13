<#escape x as x?html>
	<@bs3form.labelled_form_group path="" labelText="Set submission options">
		<@bs3form.checkbox path="collectSubmissions">
			<@f.checkbox path="collectSubmissions" id="collectSubmissions" /> Collect submissions
		</@bs3form.checkbox>
	<span class="help-block">Select this option to enable students to submit coursework for this assignment.</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Plagiarism check">
		<@bs3form.checkbox path="automaticallySubmitToTurnitin">
			<@f.checkbox path="automaticallySubmitToTurnitin" id="automaticallySubmitToTurnitin" /> Automatically check submissions for plagiarism
		</@bs3form.checkbox>
	<span class="help-block">
		Select this option to check all submissions for plagiarism when the assignment closes.
		Late submissions, or submissions within an extension, are checked for plagiarism when the submission is received.
	</span>
	<@f.hidden path="displayPlagiarismNotice" value="true" />

	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="restrictSubmissions" labelText="Submission scope">
		<@bs3form.radio>
			<@f.radiobutton path="restrictSubmissions" value="true" />
			Only students enrolled on this assignment can submit coursework
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="restrictSubmissions" value="false" />
			Anyone with a link to the assignment can submit coursework
		</@bs3form.radio>
	<span class="help-block">
		If anyone with the assignment link can submit coursework, you can't use a marking workflow.<br />
		When you restrict submissions to students enrolled on this assignment, other students who visit the assignment page can still request access.
	</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="">
		<@bs3form.checkbox path="allowResubmission">
			<@f.checkbox path="allowResubmission" id="allowResubmission" /> Allow students to re-submit work
		</@bs3form.checkbox>
	<span class="help-block">
		Select this option to allow students to submit new work, which replaces any previous submission.
		Students cannot resubmit work after the close date.
	</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Late submission">
		<@bs3form.checkbox path="allowLateSubmissions">
			<@f.checkbox path="allowLateSubmissions" id="allowLateSubmissions" /> Allow new submissions after the close date
		</@bs3form.checkbox>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Extensions">
		<@bs3form.checkbox path="allowExtensions">
			<@f.checkbox path="allowExtensions" id="allowExtensions" /> Allow extensions
		</@bs3form.checkbox>
	<span class="help-block">
		You can grant extensions for an assignment to individual students<#if department.allowExtensionRequests> and students can request extensions via Tabula</#if>.
		<#if can.do("Department.ManageExtensionSettings", department)>
			<a class="btn btn-default btn-xs use-tooltip" title="Department extension request settings (opens in a new window/tab)" href="<@routes.cm2.extensionSettings department />" target="_blank">Review</a>
		<#else>
			Departmental administrators control whether extension requests are allowed across a department.
		</#if>
	</span>
	</@bs3form.labelled_form_group>

	<#if department.allowExtensionRequests>
		<@bs3form.labelled_form_group path="">
			<@bs3form.checkbox path="extensionAttachmentMandatory">
				<@f.checkbox path="extensionAttachmentMandatory" id="extensionAttachmentMandatory" /> Students must attach at least one file to an extension request
			</@bs3form.checkbox>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="">
			<@bs3form.checkbox path="allowExtensionsAfterCloseDate">
				<@f.checkbox path="allowExtensionsAfterCloseDate"  id="allowExtensionsAfterCloseDate" /> Allow extensions after close date
			</@bs3form.checkbox>
		<span class="help-block">Select this option to permit students to request an extension for late or unsubmitted work.</span>

		</@bs3form.labelled_form_group>
	</#if>
</#escape>