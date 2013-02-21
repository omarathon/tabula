--- TAB-536

INSERT INTO GrantedRole (id, usergroup_id, builtInRoleDefinition, scope_type, scope_id)
(select ('deptadmin_' || code) as id, ownersgroup_id as usergroup_id, 'DepartmentalAdministratorRoleDefinition' as builtInRoleDefinition, 'Department' as scope_type, id as scope_id from department);

INSERT INTO GrantedRole (id, usergroup_id, builtInRoleDefinition, scope_type, scope_id)
(select ('extmanager_' || code) as id, extension_managers_id as usergroup_id, 'ExtensionManagerRoleDefinition' as builtInRoleDefinition, 'Department' as scope_type, id as scope_id from department where extension_managers_id is not null);

INSERT INTO GrantedRole (id, usergroup_id, builtInRoleDefinition, scope_type, scope_id)
(select ('modmanager_' || code) as id, participantsgroup_id as usergroup_id, 'ModuleManagerRoleDefinition' as builtInRoleDefinition, 'Module' as scope_type, id as scope_id from module where participantsgroup_id is not null); 