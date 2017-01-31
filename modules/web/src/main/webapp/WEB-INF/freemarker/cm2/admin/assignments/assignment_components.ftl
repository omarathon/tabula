<#ftl strip_text=true />

<#macro wizard_link label is_first is_active is_available tooltip="" url="">
<span class="arrow-right<#if !is_first> arrow-left</#if><#if is_active> active</#if><#if is_available && !is_active> use-tooltip</#if>" <#if is_available && !is_active>title="${tooltip}"</#if>><#compress>
	<#if is_available && !is_active>
		<a href="${url}">${label}</a>
	<#else>
	${label}
	</#if>
</#compress></span>
</#macro>

<#macro set_wizard is_new current_step>
	<p class="progress-arrows">
			<@wizard_link
				label="Assignment details"
				is_first=true
				is_active=(current_step == 'details')
				is_available=true
				tooltip="Edit properties" />

			<@wizard_link
				label="Feedback"
				is_first=false
				is_active=(current_step == 'feedback')
				is_available=false
				tooltip="Edit feedback" />

			<@wizard_link
				label="Students"
				is_first=false
				is_active=(current_step == 'students')
				is_available=false
				tooltip="Edit students" />

			<@wizard_link
				label="Markers"
				is_first=false
				is_active=(current_step == 'markers')
				is_available=false
				tooltip="Edit markers" />

			<@wizard_link
				label="Submissions"
				is_first=false
				is_active=(current_step == 'saubmissions')
				is_available=false
				tooltip="Edit submissions" />

			<@wizard_link
				label="Options"
				is_first=false
				is_active=(current_step == 'options')
				is_available=false
				tooltip="Edit options" />

			<@wizard_link
				label="Review"
				is_first=false
				is_active=(current_step == 'review')
				is_available=false
				tooltip="Review" />

		</p>
</#macro>


