<#macro academicYearSwitcher path academicYear thisAcademicYear>
	<form class="form-inline" action="${path}">
		<label>Academic year
			<select name="academicYear" class="input-small">
				<#assign academicYears = [thisAcademicYear.previous.toString, thisAcademicYear.toString, thisAcademicYear.next.toString] />
				<#list academicYears as year>
					<option <#if academicYear.toString == year>selected</#if> value="${year}">${year}</option>
				</#list>
			</select>
		</label>
		<button type="submit" class="btn btn-primary">Change</button>
	</form>
</#macro>