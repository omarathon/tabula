<#import "*/assignment_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />

<#macro review_details_header header_info tab_link>
	<div class="row">
		<div class="col-md-3"><h2>${header_info}</h2></div>
		<div class="col-md-2"><a href="${tab_link}">Edit details</a></div>
	</div>
</#macro>

<#macro review_details label_info assignment_property='' defaultValue=''>
	<div><label class="review-label">${label_info}:</label> <#if assignment_property != ''>${assignment_property}<#else>${defaultValue}</#if></div>
</#macro>
<#macro review_date_fld_details label_info assignment_property='' includeTime=true>
	<div><label class="review-label">${label_info}:</label> <#if assignment_property != ''><span class="use-tooltip" title="<@fmt.dateToWeek assignment_property />" data-html="true"><@fmt.date date=assignment_property includeTime=includeTime /></span></#if></div>
</#macro>

<#escape x as x?html>
	<@cm2.assignmentHeader "Review settings" assignment "for" />

	<div class="fix-area">
		<@components.assignment_wizard 'review' assignment.module true assignment/>
		<div class="form-group">
			<#assign detailsUrl><@routes.cm2.editassignmentdetails assignment /></#assign>

			<@review_details_header 'Assignment details' detailsUrl />
			<@review_details 'Assignment title' assignment.name />
			<@review_date_fld_details 'Open date' assignment.openDate />

			<#if assignment.openEnded>
				<@review_date_fld_details 'Open ended reminder date' assignment.openEndedReminderDate />
			<#else>
				<@review_date_fld_details 'Close date' assignment.closeDate />
			</#if>

			<@review_details 'Academic year' assignment.academicYear.toString />

			<#if assignment.cm2MarkingWorkflow??>
				<#if assignment.workflowCategory??>
					<@review_details 'Marking workflow use' assignment.workflowCategory.displayName />

					<#if assignment.workflowCategory.code == 'R'>
						<@review_details 'Marking workflow name' assignment.cm2MarkingWorkflow.name />
					</#if>
				</#if>

				<@review_details 'Marking workflow type' assignment.cm2MarkingWorkflow.workflowType.description />
			</#if>
		</div>


		<div class="form-group">
			<#assign feedbackDetailsUrl><@routes.cm2.assignmentfeedback assignment 'edit' /></#assign>
			<@review_details_header 'Feedback details' feedbackDetailsUrl />

			<#assign feedbackTemplateName>${(assignment.feedbackTemplate.name)!'None'}</#assign>
			<@review_details 'Feedback template' feedbackTemplateName />
			<@review_details 'Automatically release to markers when assignment closes or after plagiarism check' sharedPropertiesForm.automaticallyReleaseToMarkers?string('Yes','No') />
			<@review_details 'Collect marks' sharedPropertiesForm.collectMarks?string('Yes','No') />
			<@review_details 'Credit bearing' sharedPropertiesForm.summative?string('Summative','Formative') />
			<@review_details 'Dissertation' sharedPropertiesForm.dissertation?string('Yes','No') />
		</div>

		<div class="form-group">
			<#assign studentDetailsUrl><@routes.cm2.assignmentstudents assignment 'edit' /></#assign>

			<@review_details_header 'Student details' studentDetailsUrl />
			<@review_details 'SITS' assignment.upstreamAssessmentGroups?has_content?string('Linked','Not linked') />

			<div><label class="review-label">Total number of students enrolled:</label> ${membershipInfo.totalCount}
				<#if membershipInfo.excludeCount gt 0 || membershipInfo.includeCount gt 0>
					<span class="very-subtle">(${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount}
						removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, ${membershipInfo.usedIncludeCount} added manually</#if>)</span>
				<#else>
					<span class="very-subtle">from SITS</span>
				</#if>
			</div>

			<@review_details 'Anonymity' assignment.anonymousMarking?string('On (markers cannot see University IDs and names)','Off (markers can see University IDs and names)') />
		</div>

		<#if assignment.cm2MarkerAllocations?has_content>
			<div class="form-group">
				<#assign markerDetailsUrl><@routes.cm2.assignmentmarkers assignment 'edit' /></#assign>
				<@review_details_header 'Markers' markerDetailsUrl />

				<#list assignment.cm2MarkerAllocations as allocation>
					<#assign markerDetails>${allocation.marker.fullName} (<@fmt.p allocation.students?size "student" />)</#assign>
					<@review_details allocation.role markerDetails />
				</#list>
			</div>
		</#if>

		<div class="form-group">
			<#assign submissionDetailsUrl><@routes.cm2.assignmentsubmissions assignment 'edit' /></#assign>
			<@review_details_header 'Submission details' submissionDetailsUrl />
			<@review_details 'Collect submissions' sharedPropertiesForm.collectSubmissions?string('Yes','No') />

			<#if sharedPropertiesForm.collectSubmissions>
				<@review_details 'Automatically check submissions for plagiarism' sharedPropertiesForm.automaticallySubmitToTurnitin?string('Yes','No') />
				<@review_details 'Show plagiarism declaration' sharedPropertiesForm.displayPlagiarismNotice?string('Yes','No') />
				<@review_details 'Submission scope' sharedPropertiesForm.restrictSubmissions?string('Only students enrolled on this assignment can submit coursework','Anyone with a link to the assignment can submit coursework') />
				<@review_details 'Allow students to resubmit work' sharedPropertiesForm.allowResubmission?string('Yes','No') />
				<@review_details 'Allow new submissions after close date' sharedPropertiesForm.allowLateSubmissions?string('Yes','No') />
				<@review_details 'Allow extensions' sharedPropertiesForm.allowExtensions?string('Yes','No') />
				<@review_details 'Students must attach at least one file to an extension request' sharedPropertiesForm.extensionAttachmentMandatory?string('Yes','No') />
				<@review_details 'Allow extensions after close date' sharedPropertiesForm.allowExtensionsAfterCloseDate?string('Yes','No') />
			</#if>
		</div>

		<div class="form-group">
			<#assign optionDetailsUrl><@routes.cm2.assignmentoptions assignment 'edit' /></#assign>
			<@review_details_header 'Options details' optionDetailsUrl />
			<@review_details 'Minimum attachments per submission' sharedPropertiesForm.minimumFileAttachmentLimit?string />
			<@review_details 'Maximum attachments per submission' sharedPropertiesForm.fileAttachmentLimit?string />

			<#if sharedPropertiesForm.fileAttachmentTypes?has_content>
				<#assign fileTypes><#list sharedPropertiesForm.fileAttachmentTypes as fileType>${fileType?upper_case}<#if fileType_has_next>, </#if></#list></#assign>
			<#else>
				<#assign fileTypes>Any</#assign>
			</#if>
			<@review_details 'Accepted attachment file types' fileTypes />

			<#if sharedPropertiesForm.individualFileSizeLimit?has_content>
				<#assign maximumFileSize>${(sharedPropertiesForm.individualFileSizeLimit!'')?string}MB</#assign>
				<@review_details 'Maximum file size' maximumFileSize />
			<#else>
				<@review_details 'Maximum file size' 'Not specified'/>
			</#if>

			<@review_details label_info='Minimum word count' assignment_property=(sharedPropertiesForm.wordCountMin!'')?string defaultValue='None'/>
			<@review_details label_info='Maximum word count' assignment_property=(sharedPropertiesForm.wordCountMax!'')?string defaultValue='None'/>
			<@review_details label_info='Word count conventions' assignment_property=sharedPropertiesForm.wordCountConventions defaultValue='None'/>
			<div class="row">
				<div class="col-md-3"><label class="review-label">Text to show on submission form:</label></div>
				<div class="col-md-9">
					<#if sharedPropertiesForm.comment??>
						<pre class="review-dtls"><span>${sharedPropertiesForm.comment}</pre><#else>None</#if>
				</div>
			</div>
		</div>
		<div class="fix-footer">
			<a class="btn btn-default" href="<@routes.cm2.home />">Confirm</a>
		</div>
	</div>
</#escape>

<style type="text/css">
	pre.review-dtls {
		border: none;
		background-color: transparent;
		font-size: inherit;
		font-family: inherit;
		padding: inherit;
		color: inherit;
	}
</style>