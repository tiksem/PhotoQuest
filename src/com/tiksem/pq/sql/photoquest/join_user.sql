LEFT JOIN
(
  SELECT * FROM `user`
) as User_userId ON User_userId.id = photoquest.userId