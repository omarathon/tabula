<#escape x as x?html>

<h1>Edit monitoring point set template</h1>

<script>
jQuery(function($){
	var bindPointButtons = function(){
		$('a.new-point, a.edit-point, a.delete-point').off().on('click', function(e){
			e.preventDefault();
			var formLoad = function(data){
				var $m = $('#modal');
				$m.html(data);
				if ($m.find('.monitoring-points').length > 0) {
					$('.monitoring-points').replaceWith($m.find('.monitoring-points'))
					$m.modal("hide");
					bindPointButtons();
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
			$.get($(this).attr('href'), formLoad);
		});
	};
	bindPointButtons();
});
</script>

<form id="editMonitoringPointSet" action="<@url page="/sysadmin/pointsettemplates/${command.template.id}/edit"/>" method="POST" class="form-inline">

	<@spring.bind path="command.templateName">
		<p><label>Template name <input class="input" type="text" name="${status.expression}" value="${status.value}" /></label></p>
		<#if status.error>
			<div class="alert alert-error"><@f.errors path="command.templateName" cssClass="error"/></div>
		</#if>
    </@spring.bind>

	<input type="submit" value="Edit" class="btn btn-primary"/> <a class="btn" href="<@url page="/sysadmin/pointsettemplates"/>">Cancel</a>

</form>

<hr />

<#include "_monitoringPointsPersisted.ftl" />

<div id="modal" class="modal hide fade" style="display:none;"></div>

</#escape>