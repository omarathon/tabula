<div class="row">
	<div class="col-md-6">
		<h2>Search for individual students</h2>
		<div class="flexi-picker-container input-group">
			<span class="input-group-addon"><i class="icon-user fa fa-user"></i></span><#--
				--><input type="text" class="form-control flexi-picker"
						  name="singleSearch" placeholder="Enter name"
						  data-include-users="true" data-include-email="false" data-include-groups="false"
						  data-members-only="true" data-type="" autocomplete="off"
				/>
			<span class="input-group-btn"><button class="btn btn-default" type="submit" name="searchSingle" value="true"><i class="fa fa-search"></i></button></span>
		</div>

	</div>
	<div class="col-md-6">
		<h2>Search for multiple Student IDs</h2>
		<textarea class="form-control" name="multiSearch" rows="5" style="width:300px;" placeholder="Add one ID number per line">${command.multiSearch!}</textarea>
		<button class="btn btn-default" type="submit" name="searchMulti" value="true" style="vertical-align: bottom"><i class="fa fa-search"></i></button>
	</div>
</div>
<hr />