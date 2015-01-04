SELECT count(*)
FROM user
WHERE
  namedData LIKE :query
  AND location = :location
  AND gender = :gender
LIMIT 0, 200