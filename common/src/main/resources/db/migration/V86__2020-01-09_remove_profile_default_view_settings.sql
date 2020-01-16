update usersettings set settings = (settings::jsonb - 'profilesDefaultView')::text where (settings::json -> 'profilesDefaultView') is not null;
