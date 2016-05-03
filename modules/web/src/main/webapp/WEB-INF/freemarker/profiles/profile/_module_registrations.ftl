<#escape x as x?html>

<section id="module-registrations" class="clearfix">
	<#assign moduleRegsUrl><@routes.profiles.listModuleRegs studentCourseDetails.urlSafeId studentCourseYearDetails.academicYear/></#assign>
	<i class="fa fa-spinner fa-spin"></i><em> Loading modules&hellip;</em>
	<script type="text/javascript">
		jQuery(function($){
			$('#module-registrations').load('${moduleRegsUrl}', {ts: new Date().getTime()}, function( response, status, xhr )  {
				if (403 === xhr.status){
					$( "#module-registrations").html
							("<h4>Modules</h4><em>You do not have permission to view module registrations</em>" );
				} else {
					var pane = $('#module-registration-pane');
					var title = pane.find('h4').first().html();
					if (title != '' && title != undefined) {
						pane.find('.title').html(title);
						window.GlobalScripts.initCollapsible();
					}
				}
			});
		});
	</script>
</section>

</#escape>