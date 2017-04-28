<#escape x as x?html>
<#if undergraduateUpdated??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Undergraduate settings have been saved.
	</div>
</#if>

<#if postgraduateUpdated??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Postgraduate settings have been saved.
	</div>
</#if>

<h2>Manage Marks Closure</h2>

<div class="fix-area">
	<#assign actionUrl><@routes.cm2.manageMarksClosure /></#assign>
	<@f.form commandName="command" action=actionUrl>
		<@bs3form.labelled_form_group path="" labelText="">
			<@bs3form.radio>
				<@f.radiobutton path="updatePostgrads" value="false" />
					Undergraduate
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="updatePostgrads" value="true" />
					Postgraduate
			</@bs3form.radio>
		</@bs3form.labelled_form_group>

		<div id="marks-management-ug">
			<h3>Undergraduate settings</h3>
			<@department_table "ugMappings" />
		</div>

		<div id="marks-management-pg">
			<h3>Postgraduate settings</h3>
			<@department_table "pgMappings" />
		</div>

		<div class="submit-buttons fix-footer">
			<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Saving&hellip;">
				Save
			</button>
			<a class="btn btn-default" href="<@routes.coursework.home />">Cancel</a>
		</div>
	</@f.form>
</div>


<script>
	(function($) {
		var showDegreeType = function() {
			var value = $('form input[name=updatePostgrads]:checked').val();
			if (value && value === "true") {
				$('#marks-management-ug').hide();
				$('#marks-management-pg').show();
			} else {
				$('#marks-management-pg').hide();
				$('#marks-management-ug').show();
			}
		};
		$('form input[name=updatePostgrads]').on('click', showDegreeType);
		showDegreeType();

		var openAllThisYearAndLastYear = function() {
			$('form .department-years-list input[type=radio]').each(function() {
				var $radio = $(this);
				$radio.val(['openCurrentAndPrevious']);
			});
		};

		var openAllThisYear = function() {
			$('form .department-years-list input[type=radio]').each(function() {
				var $radio = $(this);
				$radio.val(['openCurrent']);
			});
		};

		var closeAll = function() {
			$('form .department-years-list input[type=radio]').each(function() {
				var $radio = $(this);
				$radio.val(['closed']);
			});
		};

		$('.selectOpenAllThisYearAndLastYear').on('click', openAllThisYearAndLastYear);
		$('.selectOpenAllThisYear').on('click', openAllThisYear);
		$('.selectCloseAll').on('click', closeAll);


	})(jQuery);
</script>

<#macro department_table map>
	<table class="department-years-list table-striped">
		<thead>
			<tr>
				<th>Dept code</th>
				<th>Department</th>
				<th>Open for ${command.previousAcademicYear.toString} and ${command.currentAcademicYear.toString} <br />
					<a href="#" class="selectOpenAllThisYearAndLastYear collection-check-all">Select all</a>
				</th>
				<th>Open for ${command.currentAcademicYear.toString} <br />
					<a href="#" class="selectOpenAllThisYear collection-check-all">Select all</a>
				</th>
				<th>Closed <br />
					<a href="#" class="selectCloseAll collection-check-all">Select all</a>
				</th>
			</tr>
		</thead>
		<tbody>
			<#list command.departments as department>
				<@department_item department "${map}[${department.code}]" />
			</#list>
		</tbody>
	</table>
</#macro>

<#macro department_item department path>
	<tr>
		<td>${(department.code!'?')?upper_case}</td>
		<td>${department.name}</td>
		<td><@f.radiobutton path="${path}" value="openCurrentAndPrevious" /></td>
		<td><@f.radiobutton path="${path}" value="openCurrent" /></td>
		<td><@f.radiobutton path="${path}" value="closed" /></td>
	</tr>
</#macro>

</#escape>