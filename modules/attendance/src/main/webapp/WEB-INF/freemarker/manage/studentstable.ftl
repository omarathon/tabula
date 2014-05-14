<#escape x as x?html>

<#if missingMembers?? && (missingMembers?size > 0)>
	<div class="alert alert-warning">
		<p>The following students could not be added:</p>
		<ul>
			<#list missingMembers as member>
				<li>${member}</li>
			</#list>
		</ul>
	</div>
</#if>

<#if (membershipItems?size > 0)>
	<span class="student-count" style="display:none;">
		<@fmt.p memberCount "student" /> on this scheme
	</span>
	<table class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tabula-darkRed tablesorter sb-no-wrapper-table-popout">
		<thead>
			<tr>
				<th style="width: 20px;">&nbsp;</th>
				<th style="width: 50px;">Source</th>
				<th>First name</th>
				<th>Last name</th>
				<th>ID</th>
				<th>User</th>
			</tr>
		</thead>
		<tbody>
			<#list membershipItems as item>
				<tr>
					<td>
					</td>
					<td>
						<#if item.itemTypeString == "static">
							<span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span>
							<input type="hidden" name="staticStudentIds" value="${item.member.universityId}" />
						<#elseif item.itemTypeString == "exclude">
							<span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-ban-circle"></i></span>
							<input type="hidden" name="excludedStudentIds" value="${item.member.universityId}" />
						<#else>
							<span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span>
							<input type="hidden" name="includedStudentIds" value="${item.member.universityId}" />
						</#if>
					</td>
					<td>${item.member.firstName}</td>
					<td>${item.member.lastName}</td>
					<td>${item.member.universityId}</td>
					<td>${item.member.userId}</td>
				</tr>
			</#list>
		</tbody>
	</table>
</#if>

</#escape>