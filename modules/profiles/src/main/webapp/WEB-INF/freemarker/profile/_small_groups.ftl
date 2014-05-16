<#if !groupsWidgetUrl?has_content><#assign groupsWidgetUrl = '/groups/student/${profile.universityId}' /></#if>

<section id="small-groups" class="clearfix" >
	<script type="text/javascript">
		jQuery(function($){
			$('#small-groups').load('${groupsWidgetUrl}', {ts: new Date().getTime()}, function() {
				var pane = $('#sg-pane');
				var title = pane.find('h4').html();
				if (title != '' && title != undefined) {
					pane.find('.title').html(title);
					$('a.ajax-modal', '#small-groups').ajaxModalLink();
					$('#sg-pane').show();
				}
			});
		});
	</script>
</section>