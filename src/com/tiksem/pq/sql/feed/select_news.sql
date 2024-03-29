SELECT action.*, photoquest.*, photo.*, user.*
FROM feed, action, photoquest, photo, user
WHERE feed.userId = :userId 
      AND action.id = feed.actionId
      AND photoquest.id = action.photoquestId
      AND photo.id = action.photoId
      AND user.id = action.userId
LIMIT :offset, :limit;