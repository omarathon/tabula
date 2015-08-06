<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		body {font-family: "Helvetica Neue", Helvetica, Arial, sans-serif
		}
	</style>
	<title>${feedback.assignment.module.name} (${feedback.assignment.module.code?upper_case}) - ${feedback.assignment.name}</title>
</head>
<body>
<h2>${feedback.assignment.module.name} (${feedback.assignment.module.code?upper_case})</h2>
<h2>${feedback.assignment.name}</h2>
<h3>Feedback for ${user.warwickId} </h3>

<#if feedback.hasMarkOrGrade>
	<div class="mark-and-grade">
		<#if feedback.studentViewableAdjustments?has_content && feedback.latestMark??>
			<h3>Adjusted mark: ${feedback.latestMark}%</h3>
		<#elseif feedback.latestMark??>
			<h3>Mark: ${feedback.latestMark}%</h3>
		</#if>
		<#if feedback.studentViewableAdjustments?has_content && feedback.latestGrade??>
			<h3>Adjusted grade: ${feedback.latestGrade}</h3>
		<#elseif feedback.latestGrade??>
			<h3>Grade: ${feedback.latestGrade}</h3>
		</#if>
	</div>
</#if>
	<#list feedback.studentViewableAdjustments as viewableFeedback>
		<#if viewableFeedback??>
		<div class="alert">
			<p>
				<strong>${viewableFeedback.reason}</strong> - An adjustment has been made to your final mark. The
				mark shown above will contribute to your final module mark.
			</p>
			<#if viewableFeedback.comments??><p>${viewableFeedback.comments}</p></#if>
			<p>Your marks before adjustment were:</p>

			<#if viewableFeedback_has_next>
				<#if feedback.studentViewableAdjustments[viewableFeedback_index +1].mark??><div>Mark: ${feedback.studentViewableAdjustments[viewableFeedback_index +1].mark}%</div></#if>
				<#if feedback.studentViewableAdjustments[viewableFeedback_index +1].grade??><div>Grade: ${feedback.studentViewableAdjustments[viewableFeedback_index +1].grade}</div></#if>
			<#else>
				<div>Mark: ${feedback.studentViewableRawMark}</div>
				<#if feedback.studentViewableRawGrade??><div>Grade: ${feedback.studentViewableRawGrade}</div></#if>
			</#if>
		</div>
		</#if>
	</#list>


<#if  feedback.assignment.genericFeedback??>
<div class="feedback-notes">
<h4>Feedback for all students on this assignment</h4>
${feedback.assignment.genericFeedback!""}
</div>
</#if>
<#if feedback.comments??>
<div class="feedback-notes">
<h4>Feedback on your submission</h4>
${feedback.comments!""}
</div>
</#if>

<#if feedback.attachments?has_content>
	<div class="feedback-notes">
		<h4>Please note there <@fmt.p number=feedback.attachments?size singular="is" plural="are" shownumber=false /> also <@fmt.p number=feedback.attachments?size singular="feedback file" plural="feedback files" shownumber=true /> available for download.</h4>
	</div>
</#if>

</body>
</html>
</#escape>