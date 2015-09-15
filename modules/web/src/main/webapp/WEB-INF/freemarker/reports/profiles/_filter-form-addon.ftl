<div class="row">
	<div class="col-md-6">
		<h2>Search for individual students</h2>
		<@bs3form.flexipicker name="singleSearch" placeholder="Enter name or ID" membersOnly="true">
			<span class="input-group-btn"><button class="btn btn-default" type="submit" name="searchSingle" value="true"><i class="fa fa-search"></i></button></span>
		</@bs3form.flexipicker>
	</div>
	<div class="col-md-6">
		<h2>Search for multiple Student IDs</h2>
		<textarea class="form-control" name="multiSearch" rows="5" style="width:300px;" placeholder="Add one ID number per line">${command.multiSearch!}</textarea>
		<button class="btn btn-default" type="submit" name="searchMulti" value="true" style="vertical-align: bottom"><i class="fa fa-search"></i></button>
	</div>
</div>
<hr />