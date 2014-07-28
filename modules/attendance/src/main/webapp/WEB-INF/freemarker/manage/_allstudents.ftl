<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#if membershipItems?size == 0>
	<div class="students"></div>
<#else>
	<div class="students">
		<@attendance_macros.manageStudentTable membershipItems />
	</div>
	<script>
		jQuery('a.ajax-modal').ajaxModalLink();
	</script>
</#if>

</#escape>
