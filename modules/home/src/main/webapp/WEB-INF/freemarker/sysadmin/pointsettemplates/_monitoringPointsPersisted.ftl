<div class="monitoring-points">
	<#if command.template.points?size == 0>
		<div class="striped-section">
			<div class="pull-right">
				<a href="<@url page="/sysadmin/pointsettemplates/${command.template.id}/points/add" />" class="btn btn-primary new-point"><i class="icon-plus"></i> Create new point</a>
			</div>
			<h2 class="section-title">Monitoring points</h2>
			<div class="striped-section-contents">
				<div class="item-info">
					<em>No points exist for this template</em>
				</div>
			</div>
		</div>
	<#else>
		<div class="striped-section">
			<div class="pull-right">
            	<a href="<@url page="/sysadmin/pointsettemplates/${command.template.id}/points/add" />" class="btn btn-primary new-point"><i class="icon-plus"></i> Create new point</a>
            </div>
			<h2 class="section-title">Monitoring points</h2>
        	<div class="striped-section-contents">
        		<#list command.template.points as point>
            		<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@url page="/sysadmin/pointsettemplates/${command.template.id}/points/${point.id}/edit"/>">Edit</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@url page="/sysadmin/pointsettemplates/${command.template.id}/points/${point.id}/delete"/>"><i class="icon-remove"></i></a>
							</div>
							${point.name} (week ${point.week})
						</div>
					</div>
            	</#list>
        	</div>
        </div>
	</#if>
</div>