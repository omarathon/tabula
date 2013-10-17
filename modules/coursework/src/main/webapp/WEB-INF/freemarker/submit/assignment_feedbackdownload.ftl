<h2>Feedback for ${user.universityId}</h2>
	
<#if features.collectRatings && feedback.collectRatings>
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

<#if  assignment.genericFeedback??>
<div class="feedback-notes">
<h3>General feedback on the assignment:</h3> ${assignment.genericFeedback!""}
</div>
</#if>
<#if feedback.defaultFeedbackComments??>
<div class="feedback-notes">
<h3>Feedback on your submission</h3> ${feedback.defaultFeedbackComments!""}
</div>
</#if>

<p>
	<#assign feedbackcount=feedback.attachments?size>
	<#-- Only offer a Zip if there's more than one file. -->
	<#if feedbackcount gt 1>
		<p>Your feedback consists of ${feedback.attachments?size} files.</p>
		<p>
			<a class="btn btn-success" href="<@url page="/module/${module.code}/${assignment.id}/all/feedback.zip"/>"><i class="icon-gift"></i>
				Download all as a Zip file
			</a>
		</p>
		<p>Or download the attachments individually below.</p>
	<#elseif feedbackcount gt 0>
		<p>Your feedback file is available to download below.</p>
	</#if>

	<#if feedback.attachments?has_content>
		<ul class="file-list">
		<#list feedback.attachments as attachment>
			<li>
			<a class="btn<#if feedbackcount=1> btn-success</#if>" href="<@url page="/module/${module.code}/${assignment.id}/get/${attachment.name?url}"/>"><i class="icon-file"></i>
				${attachment.name}
			</a>
			</li>
		</#list>
		</ul>
	</#if>
</p>

<a href="<@url page="/module/${module.code}/${assignment.id}/feedback.pdf"/>"> Download feedback as a PDF file</a>