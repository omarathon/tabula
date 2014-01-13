<#macro profile_link user>
	<a class="use-popover profile-link"
	   id="popover-${user.warwickId}"
	   data-html="true"
	   data-original-title="<span class='text-info'><strong>University ID</strong></span>"
	   data-content="${user.warwickId}">
		<i class="icon-info-sign"></i>
	</a>
</#macro>
