alter table mitigatingcircumstancessubmission
  add column outcomeGrading varchar(255),
  add column outcomeReasons bytea,
  add column boardRecommendations varchar(255)[],
  add column boardRecommendationOther bytea,
  add column boardRecommendationComments bytea;
