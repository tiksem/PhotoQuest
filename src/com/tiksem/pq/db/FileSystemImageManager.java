package com.tiksem.pq.db;

import com.utils.framework.imagemagick.ImageMagickExecutor;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by CM on 11/29/2014.
 */
public class FileSystemImageManager implements ImageManager {
    private static final int MAX_IMAGE_PATH_LENGTH =
            String.valueOf(Long.MAX_VALUE).length();
    private static final int DIRECTORY_NAME_LENGTH = 3;

    private String imageDirectory;
    private ImageMagickExecutor imageMagickExecutor;

    public FileSystemImageManager(String imageDirectory, String imageMagickPath) {
        this.imageDirectory = imageDirectory;
        imageMagickExecutor = new ImageMagickExecutor(imageMagickPath);
    }

    private String generateImagePath(long id) {
        String path = String.valueOf(id);
        path = Strings.repeat('0', MAX_IMAGE_PATH_LENGTH - path.length()) + path;
        path = Strings.join("/",
                Strings.splitInStringsWithLength(path, DIRECTORY_NAME_LENGTH)).toString();
        path = imageDirectory + "/" + path;
        return path;
    }

    private String generateThumbnailImagePath(String imagePath, int size) {
        return imagePath + "th" + size;
    }

    @Override
    public InputStream getImageById(long id) {
        String path = generateImagePath(id);
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public void saveImage(long id, InputStream inputStream) {
        String path = generateImagePath(id);
        new File(path).getParentFile().mkdirs();
        try {
            IOUtils.copy(inputStream, new FileOutputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getThumbnailOfImage(long id, int size) {
        String imagePath = generateImagePath(id);
        if(!new File(imagePath).exists()){
            return null;
        }

        String thumbnailPath = generateThumbnailImagePath(imagePath, size);
        try {
            return new FileInputStream(thumbnailPath);
        } catch (FileNotFoundException e) {
            try {
                imageMagickExecutor.createSquareThumbnail(imagePath, thumbnailPath, size);
                return new FileInputStream(thumbnailPath);
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
    }
}
