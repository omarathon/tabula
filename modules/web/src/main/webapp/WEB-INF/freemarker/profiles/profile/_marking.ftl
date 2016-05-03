<#escape x as x?html>

<section id="marking-details" class="marking-detail clearfix">
	<h4>Coursework Marking</h4>
	<#list marking as assignment>
		<div class="simple-assignment-info">
			<div>
				<span class="mod-code">${assignment.module.code?upper_case}</span>
				<span class="mod-name">${assignment.module.name}</span>
			</div>
			<div class="alert alert-info clearfix">
				<div class="marking-detail-name"><div>${assignment.name}</div></div>
				<div>
					<a class="btn btn-xs btn-default" href="<@routes.profiles.listmarkersubmissions assignment marker />">
						Manage
					</a>
				</div>
			</div>
		</div>
	</#list>
</section>

</#escape>