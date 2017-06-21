<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>
	<@modal.wrapper>
		<@modal.header>
			<h3 class="modal-title">Feedback Summary - ${feedback.studentIdentifier}</h3>
		</@modal.header>
		<@modal.body>
			<div class="form onlineFeedback">
				<form>
					<#if assignment.genericFeedback??>
						<@bs3form.labelled_form_group labelText="Generic feedback">
							<#noescape>${assignment.genericFeedbackFormattedHtml!}</#noescape>
						</@bs3form.labelled_form_group>
					</#if>

					<#if feedback.customFormValues?has_content>
						<@bs3form.labelled_form_group labelText="Feedback">
							<#list feedback.customFormValues as formValue>
								<#noescape>${formValue.valueFormattedHtml!""}</#noescape>
							</#list>
						</@bs3form.labelled_form_group>
					</#if>

					<#if assignment.collectMarks>
						<@bs3form.labelled_form_group labelText="Mark">
							<div class="input-group col-xs-4">
								<input type="text" disabled="disabled" value="${feedback.actualMark!""}" class="form-control">
								<div class="input-group-addon">%</div>
							</div>
						</@bs3form.labelled_form_group>

						<@bs3form.labelled_form_group labelText="Grade">
							<input type="text" disabled="disabled" value="${feedback.actualGrade!""}" class="form-control">
						</@bs3form.labelled_form_group>
					</#if>

					<#if feedback.attachments?has_content>
						<@bs3form.labelled_form_group labelText="Attachments">
							<#if feedback.attachments?size == 1>
								<#assign attachmentExtension = feedback.attachments[0].fileExt>
							<#else>
								<#assign attachmentExtension = "zip">
							</#if>

							<a href="<@routes.cm2.feedbackDownload feedback attachmentExtension />">
								<@fmt.p number=feedback.attachments?size singular="file" />
							</a>
						</@bs3form.labelled_form_group>
					</#if>
				</form>
			</div>
		</@modal.body>
	</@modal.wrapper>
</#escape>