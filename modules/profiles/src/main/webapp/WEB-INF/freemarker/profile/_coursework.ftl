<#escape x as x?html>

<section id="coursework" class="clearfix">
	<#if can.do("Profiles.Read.Coursework", profile)>
		<script type="text/javascript">
			jQuery(function($){
				$('#coursework').load('/coursework/student/bycourseandyear/${studentCourseYearDetails.id}', {ts: new Date().getTime()}, function() {
					var pane = $('#coursework-pane');
					var title = pane.find('h4').first().html();
					if (title != '' && title != undefined) {
						pane.find('.title').html(title);
						window.GlobalScripts.initCollapsible();
					}
				});
			});
		</script>
	<#else>
		<h4>${profile.firstName}'s assignments</h4>
		<em>You do not have permission to view ${profile.firstName}'s assignments</em>
		<script>
			var pane = jQuery('#coursework-pane');
			var title = pane.find('h4').first().html();
			if (title != '' && title != undefined) {
				pane.find('.title').html(title);
			}
		</script>
	</#if>
</section>

</#escape>