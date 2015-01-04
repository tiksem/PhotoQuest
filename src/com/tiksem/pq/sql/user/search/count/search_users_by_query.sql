SELECT count(*)
FROM user
WHERE
  namedData LIKE :query
LIMIT 0, 200
