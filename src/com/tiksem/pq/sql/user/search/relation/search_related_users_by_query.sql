SELECT ::select FROM user
  JOIN
  (
    SELECT relationship.::selectUserIdColumn as userId
    FROM relationship
    WHERE ::userIdColumn = :userId AND `type` = :type
    LIMIT 0, :innerLimit
  ) as sel ON sel.userId = user.ID
WHERE nameData LIKE :query
::where
::orderByLimitExpression