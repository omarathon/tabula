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
						$('#coursework-pane').show();
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
				$('#coursework-pane').show();
			}
		</script>
	</#if>
</section>