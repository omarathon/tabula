<#escape x as x?html>

<h1>Create monitoring point set template</h1>

<script>
jQuery(function($){
	var pointsChanged = false;

	var bindPointButtons = function(){
		$('#addMonitoringPointSet a.new-point, #addMonitoringPointSet a.edit-point, #addMonitoringPointSet a.delete-point').off().on('click', function(e){
			e.preventDefault();
			var formLoad = function(data){
				var $m = $('#modal');
				$m.html(data);
				if ($m.find('.monitoring-points').length > 0) {
					$('#addMonitoringPointSet .monitoring-points').replaceWith($m.find('.monitoring-points'))
					$m.modal("hide");
					bindPointButtons();
					pointsChanged = true;
					return;
				}
				var $f = $m.find('form');
				$f.on('submit', function(event){
					event.preventDefault();
				});
				$m.find('.modal-footer button[type=submit]').on('click', function(){
					$(this).button('loading');
					$.post($f.attr('action'), $f.serialize(), function(data){
						$(this).button('reset');
						formLoad(data);
					})
				});
				$m.off('shown').on('shown', function(){
					$f.find('input[name="name"]').focus();
				}).modal("show");
			};
			$.post($(this).attr('href'), $('form#addMonitoringPointSet').serialize(), formLoad)
		});
	};
	bindPointButtons();

	$('#addMonitoringPointSet').on('submit', function(){
		pointsChanged = false;
	})

	if ($('#addMonitoringPointSet').length > 0) {
		$(window).bind('beforeunload', function(){
			if (pointsChanged) {
				return 'You have made changes to the monitoring points. If you continue they will be lost.'
			}
		});
	}
});
</script>

<form id="addMonitoringPointSet" action="<@url page="/sysadmin/pointsettemplates/add"/>" method="POST" class="form-inline">

	<@spring.bind path="command.templateName">
		<p><label>Template name <input class="input" type="text" name="${status.expression}" value="${status.value}" /></label></p>
		<#if status.error>
			<div class="alert alert-error"><@f.errors path="command.templateName" cssClass="error"/></div>
		</#if>
    </@spring.bind>

	<#include "_monitoringPoints.ftl" />

	<input type="submit" value="Create" class="btn btn-primary"/> <a class="btn" href="<@url page="/sysadmin/pointsettemplates"/>">Cancel</a>

</form>

<div id="modal" class="modal hide fade" style="display:none;"></div>

</#escape>