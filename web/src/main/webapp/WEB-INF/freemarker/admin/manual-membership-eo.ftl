<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#escape x as x?html>

	<#function route_function dept>
		<#local result><@routes.admin.manualmembership dept /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Manual membership summary" route_function "for" />

	<p>
		This page shows a list of all ${department.name} assignments and small group sets in ${academicYear} that have manually added students.
	</p>

	<#if studentsByModule?has_content>
	<div>
		<#list studentsByModule?keys as module>
			<h4>
				<span class="mod-code">${module.code}</span>
				<span class="mod-name">${module.name}</span>
			</h4>

			<table class="table table-bordered table-striped table-condensed sortable">
				<thead><tr><th class="sortable">ID</th><th class="sortable">Usercode</th><th class="sortable">First name</th><th class="sortable">Last name</th></tr></thead>
				<tbody><#list  mapGet(studentsByModule, module) as student>
				<tr>
					<td>${student.warwickId}</td>
					<td>${student.userId}</td>
					<td>${student.firstName}</td>
					<td>${student.lastName}</td>
				</tr>
				</#list></tbody>
			</table>
		</#list>
	</div>
	</#if>

	<script type="text/javascript">
		jQuery(function($){
			$('table.sortable').sortableTable();
		});
	</script>

</#escape>