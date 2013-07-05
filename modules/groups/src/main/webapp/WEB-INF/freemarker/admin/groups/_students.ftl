<#escape x as x?html>

<#-- Members picker is pretty hefty so it is in a separate file -->
<#if editSmallGroupSetCommand??>
		<#assign command=editSmallGroupSetCommand />
<#else>
		<#assign command=createSmallGroupSetCommand />
</#if>

<#include "groups_membership_picker.ftl" />

<script>
	jQuery(function ($) {
		$('#action-submit').closest('form').on('click', '.update-only', function() {
			$('#action-submit').val('update');
		});
	});
</script>
</#escape>