--- TAB-536

CREATE INDEX IDX_GRANTEDROLE_TYPE ON GRANTEDROLE(scope_type, scope_id, custom_role_id, builtInRoleDefinition);
CREATE INDEX IDX_GRANTEDPERMISSION_TYPE ON GRANTEDPERMISSION(scope_type, scope_id, permission, overrideType);