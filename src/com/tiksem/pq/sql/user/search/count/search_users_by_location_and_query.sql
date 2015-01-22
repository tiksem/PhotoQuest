SELECT count(*)
FROM user
WHERE
  nameData LIKE :query
  AND location = :location
LIMIT 0, 200