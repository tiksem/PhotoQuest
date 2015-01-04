SELECT count(*)
FROM user
WHERE
  namedData LIKE :query
  AND location = :location
LIMIT 0, 200