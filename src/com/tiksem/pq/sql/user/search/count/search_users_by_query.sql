SELECT count(*)
FROM user
WHERE
  nameData LIKE :query
  :where
LIMIT 0, :innerLimit
