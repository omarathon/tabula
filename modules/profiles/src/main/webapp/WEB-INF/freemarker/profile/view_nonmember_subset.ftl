<div class="modal-header">
	<a class="close" data-dismiss="modal" aria-hidden="true">&times;</a>
	<h1>${studentUser.fullName}</h1>
</div>
<div class="modal-body">
	<table class="profile-or-course-info">
		<tbody>
			<tr>
				<th>Email</th>
				<td>
					<a href="mailto:${studentUser.email}">${studentUser.email}</a>
				</td>
			</tr>
			<tr>
				<th>University number</th>
				<td>${studentUser.warwickId}</td>
			</tr>
		</tbody>
	</table>
	<div class="alert alert-info">
		Further details for this user are not available in Tabula.
	</div>
</div>