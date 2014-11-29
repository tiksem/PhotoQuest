package com.tiksem.pq.db;

import com.utils.framework.strings.Strings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by CM on 11/29/2014.
 */
public class FileSystemImageManager implements ImageManager {
    private static final int MAX_IMAGE_PATH_LENGTH =
            String.valueOf(Long.MAX_VALUE).length();
    private static final int DIRECTORY_NAME_LENGTH = 3;

    private String imageDirectory;

    public FileSystemImageManager(String imageDirectory) {
        this.imageDirectory = imageDirectory;
    }

    private String generateImagePath(long id) {
        String path = String.valueOf(id);
        path = Strings.repeat('0', MAX_IMAGE_PATH_LENGTH - path.length()) + path;
        path = Strings.join("/",
                Strings.splitInStringsWithLength(path, DIRECTORY_NAME_LENGTH)).toString();
        path = imageDirectory + "/" + path;
        return path;
    }

    @Override
    public byte[] getImageById(long id) {
        String path = generateImagePath(id);
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void saveImage(long id, byte[] value) {
        String path = generateImagePath(id);
        new File(path).getParentFile().mkdirs();
        try {
            Files.write(Paths.get(path), value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
