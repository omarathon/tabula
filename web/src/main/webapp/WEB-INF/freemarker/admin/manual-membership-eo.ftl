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

	<#if studentModuleMap?has_content>
		<table class="table table-bordered table-striped table-condensed sortable">
			<thead><tr><th class="sortable">ID</th><th class="sortable">First name</th><th class="sortable">Last name</th><th>Modules</th></tr></thead>
			<tbody>
				<#list studentModuleMap?keys as student>
				<tr>
					<td>${student.warwickId}</td>
					<td>${student.firstName}</td>
					<td>${student.lastName}</td>
					<td><#list mapGet(studentModuleMap, student) as module>${module.code}<#if module_has_next>, </#if></#list></td>
				</tr>
				</#list>
			</tbody>
		</table>
	</#if>

	<script type="text/javascript">
		jQuery(function($){
			$('table.sortable').sortableTable();
		});
	</script>

</#escape>