<div class="row-fluid">
	<div class="span12">
		<form action="<@url page="/manage/${departmentCode}/sets"/>" class="manage-points-academicyear form-inline">
			<select name="academicyear">
				<option value="" disabled selected style="display:none;">Academic year</option>
				<option value="${academicYear.previous.toString}">${academicYear.previous.toString}</option>
                <option value="${academicYear.toString}">${academicYear.toString}</option>
                <option value="${academicYear.next.toString}">${academicYear.next.toString}</option>
			</select>
		</form>
	</div>
</div>
<script>
	Attendance.Manage.bindChooseAcademicYear();
</script>