<#-- 

This must include all the fields that are included
in _common_fields.ftl or else some values will get
lost between requests.

-->

<#-- Outputs false as the value if the value is missing. -->
<#macro hiddenorfalse path>
	<@spring.bind path=path>
	<#local value=status.value!'false' />
	<#if value=''>
		<#local value="false" />
	</#if>
	<input type="hidden" name="${status.expression}" value="${value}" />
	</@spring.bind>
</#macro>

<@hiddenorfalse path="collectSubmissions" />
<@hiddenorfalse path="collectMarks" />
<@hiddenorfalse path="displayPlagiarismNotice" />
<@hiddenorfalse path="restrictSubmissions" />
<@hiddenorfalse path="allowLateSubmissions" />
<@hiddenorfalse path="allowResubmission" />
<@hiddenorfalse path="allowExtensions" />
<@hiddenorfalse path="allowExtensionRequests" />
<@f.hidden path="wordCountMin" />
<@f.hidden path="wordCountMax" />
<@f.hidden path="wordCountConventions" />
<@f.hidden path="fileAttachmentLimit" />
<@f.hidden path="fileAttachmentTypes" />
<@f.hidden path="comment" />
<@f.hidden path="feedbackTemplate" />
<@f.hidden path="markingWorkflow" />