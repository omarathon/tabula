<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>
	<#list moduleInfo.assignments as info>
		<span id="admin-assignment-container-${info.assignment.id}">
			<@components.admin_assignment_info info skeleton=true/>
		</span>

		<script type="text/javascript">
			jQuery(function($) {

				var detailUrl =  $('.admin-assignment-${info.assignment.id}').data('detailurl');
				var $container = $('#admin-assignment-container-${info.assignment.id}');

				$container.data('request', $.ajax({
					url: detailUrl,
					statusCode: {
						403: function () {
							$container.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
						}
					},
					success: function (data) {
						$container.html(data);
						// bind form helpers
						$container.bindFormHelpers();
						$container.data('loaded', true);
						$container.trigger('tabula.formLoaded');
					},
					error: function () {
						$container.html("<p>No data is currently available. Please check that you are signed in.</p>");
					}
				}));
			});
		</script>
	</#list>
</#escape>