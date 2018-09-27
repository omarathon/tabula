<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<#assign skeleton = skeleton!false />
	<#list moduleInfo.assignments as info>
		<span id="admin-assignment-container-${info.assignment.id}">
			<@components.admin_assignment_info info skeleton=skeleton/>
		</span>

		<#if skeleton>
			<script type="text/javascript">
				jQuery(function($) {
					var detailUrl =  $('.admin-assignment-${info.assignment.id}').data('detailurl');
					var $content = $('.admin-assignment-${info.assignment.id}');

					$content.data('request', $.ajax({
						url: detailUrl,
						statusCode: {
							403: function () {
								$content.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
							}
						},
						success: function (data) {
							$content.html(data);
							// bind form helpers
							$content.bindFormHelpers();
							$content.data('loaded', true);
							$content.trigger('tabula.formLoaded');
						},
						error: function () {
							$content.html("<p>No data is currently available. Please check that you are signed in.</p>");
						}
					}));
				});
			</script>
		</#if>
	</#list>
</#escape>