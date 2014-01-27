<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
	<h2 class="section">Administration</h2>

	<div class="row-fluid">
		<div class="span6">
			<h6>Late &amp; suspicious activity</h6>
			
			<#if activities?has_content && activities.total gt 0>
				<table class="table table-condensed table-hover" id="activities">
					<#include "activities.ftl" />
					
					<tfoot aria-hidden="true" id="activity-fetcher" style="display:none;">
						<tr><td>
							<a href="#">See more</a>
						</td></tr>
					</tfoot>
				</table>
			<#else>
				<p class="alert">There is no notable activity to show you right now.</p>
			</#if>
		</div>
		
		<div class="span6">
			<#if nonempty(ownedModuleDepartments)>
				<h6>My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h6>
				
				<ul class="links">
					<#list ownedModuleDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>

			<#if nonempty(ownedDepartments)>
				<h6>My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h6>
			
				<ul class="links">
					<#list ownedDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>
		</div>
	</div>
	
	<script type="text/javascript">
		(function ($) {
			if ($("#activities").length > 0) {
				var sUrl = $("#activities").data("url");
				$("#activity-fetcher").show().click(function(e) {
					e.preventDefault();
					
					$.get(sUrl, function(pagelet) {
						$("#activities tbody:last").after(pagelet);

						$("#activity-fetcher").remove();
						
						$(".streaming").fadeIn("normal", function() {
							var $streaming = $(this);
							$streaming.replaceWith($streaming.contents());
						});
					});
				});
			}
		})(jQuery);
	</script>
</#if>