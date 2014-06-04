<h2>Feedback for ${feedback.universityId}</h2>
	
<#if features.collectRatings && feedback.collectRatings && isSelf>
	<div id="feedback-rating-container" class="is-stackable">
		<!-- fallback for noscript -->
		<div style="padding:0.5em">
		<a target="_blank" href="<@routes.ratefeedback feedback />">Rate your feedback</a> (opens in a new window/tab)
		</div>
	</div>
</#if>

<#if feedback.hasMarkOrGrade>
	<div class="mark-and-grade">
		<#if feedback.actualMark??><h3>Mark: ${feedback.actualMark}</h3></#if>
		<#if feedback.actualGrade??><h3>Grade: ${feedback.actualGrade}</h3></#if>
	</div>
</#if>

<#if assignment.genericFeedback??>
<div class="feedback-notes">
<h3>General feedback on the assignment:</h3> ${assignment.genericFeedback!""}
</div>
</#if>
<#if feedback.comments??>
<div class="feedback-notes">
	<h3>Feedback on
	<#if isSelf>
		your
	<#else>
		the student's
	</#if>
	 submission</h3> ${feedback.comments!""}
</div>
</#if>

<p>
	<#assign feedbackcount=feedback.attachments?size>
	<#-- Only offer a Zip if there's more than one file. -->
	<#if feedbackcount gt 1>
		<p>
			<#if isSelf>
				Your
			<#else>
				The student's
			</#if>
			 feedback consists of ${feedback.attachments?size} files.</p>
		<p>
			<#assign zipDownloadUrl><#compress>
				<#if isSelf>
					<@routes.feedbackZip feedback />
				<#else>
					<@routes.feedbackZip_in_profile feedback />
				</#if>
			</#compress></#assign>

			<a class="btn btn-success" href="${zipDownloadUrl}"><i class="icon-gift"></i>
				Download all as a Zip file
			</a>
		</p>
		<p>Or download the attachments individually below.</p>
	<#elseif feedbackcount gt 0>
		<p>
			<#if isSelf>
				Your
			<#else>
				The student's
			</#if>
			 feedback file is available to download below.</p>
	</#if>

	<#if feedback.attachments?has_content>
		<ul class="file-list">
		<#list feedback.attachments as attachment>
			<li>
				<#assign attachmentDownloadUrl><#compress>
					<#if isSelf>
						<@routes.feedbackAttachment feedback attachment />
					<#else>
						<@routes.feedbackAttachment_in_profile feedback attachment />
					</#if>
				</#compress></#assign>

				<a class="btn<#if feedbackcount=1> btn-success</#if>" href="${attachmentDownloadUrl}"><i class="icon-file"></i>
					${attachment.name}
				</a>
			</li>
		</#list>
		</ul>
	</#if>

</p>

<#if feedback.hasOnlineFeedback || feedback.hasMarkOrGrade || feedback.comments?? || assignment.genericFeedback??>
	<a href="<@routes.feedbackPdf assignment=assignment />"> Download<#if feedback.attachments?has_content> additional</#if> feedback as a PDF file</a>
</#if>


