<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		body {font-family: "Helvetica Neue", Helvetica, Arial, sans-serif 
		}
	</style>
	<title>Test document</title>
</head>
<body>
<h2>Feedback for ${user.universityId} on ${feedback.assignment.module.code}</h2>

<#if feedback.hasMarkOrGrade>
	<div class="mark-and-grade">
		<#if feedback.actualMark??><h3>Mark: ${feedback.actualMark}</h3></#if>
		<#if feedback.actualGrade??><h3>Grade: ${feedback.actualGrade}</h3></#if>
	</div>
</#if>

<#if  feedback.assignment.genericFeedback??>
<div class="feedback-notes">
<h3>General feedback on the assignment:</h3> ${feedback.assignment.genericFeedback!""}
</div>
</#if>
<#if feedback.defaultFeedbackComments??>
<div class="feedback-notes">
<h3>Feedback on your submission</h3> ${feedback.defaultFeedbackComments!""}
</div>
</#if>



</body>
</html>
</#escape>