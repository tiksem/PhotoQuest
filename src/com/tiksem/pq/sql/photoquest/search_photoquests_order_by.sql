SELECT * FROM photoquest
  JOIN 
  (
        SELECT photoquestId, MATCH (keywords) AGAINST (:query IN NATURAL LANGUAGE MODE) as relevance
        FROM photoquestsearch, photoquest
        WHERE
          MATCH (keywords) AGAINST (:query IN NATURAL LANGUAGE MODE)
          AND photoquest.ID = photoquestsearch.photoquestId
        ORDER BY relevance desc LIMIT 0, 100
  ) as sel ON sel.photoquestId = photoquest.ID
ORDER BY :orderBy LIMIT :offset, :limit