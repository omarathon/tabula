<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<#list modules as moduleInfo>
		<@components.admin_assignment_list moduleInfo.module moduleInfo.assignments false />
	</#list>

	<script type="text/javascript">
		// We probably just grew a scrollbar, so let's trigger a window resize
		(function ($) {
			$(window).trigger('resize.ScrollToFixed');
		})(jQuery);
	</script>
</#escape>