SELECT count(*)
FROM photoquestsearch
WHERE
  MATCH (keywords) AGAINST (:query IN NATURAL LANGUAGE MODE) 
LIMIT 0, 100