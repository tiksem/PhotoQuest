package com.tiksem.pq.db;

import com.tiksem.pq.exceptions.AspectMaxFactorRatioException;
import com.tiksem.pq.exceptions.AspectRatioData;
import com.utils.framework.imagemagick.ImageMagickExecutor;
import com.utils.framework.imagemagick.Size;
import com.utils.framework.strings.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;

import java.io.*;
import java.util.Iterator;

/**
 * Created by CM on 11/29/2014.
 */
public class FileSystemImageManager implements ImageManager {
    private static final int MAX_IMAGE_PATH_LENGTH =
            String.valueOf(Long.MAX_VALUE).length();
    private static final int DIRECTORY_NAME_LENGTH = 3;
    private static final double MAX_ASPECT_RATIO_K = 5.0;
    private static final int MAX_WIDTH = 900;
    private static final int MAX_HEIGHT = 900;

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
    public InputStream getImageById(long id, int maxWidth, int maxHeight) {
        String path = generateImagePath(id);
        String maxPath = path + "max" + maxWidth + "x" + maxHeight;
        try {
            return new FileInputStream(maxPath);
        } catch (FileNotFoundException e) {
            if(new File(path).exists()){
                try {
                    String result = imageMagickExecutor.getImage(path).resizeProportionallyFitMax(
                            new Size(maxWidth, maxHeight), maxPath);
                    return new FileInputStream(result);
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public void deleteImage(final long id) {
        String path = generateImagePath(id);
        Iterator<File> iterator = FileUtils.iterateFiles(new File(path).getParentFile(), new AbstractFileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith(String.valueOf(id));
            }
        }, new AbstractFileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }
        });

        try {
            while (iterator.hasNext()) {
                FileUtils.forceDelete(iterator.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fixDimensions(String path) throws IOException {
        ImageMagickExecutor.Image image = imageMagickExecutor.getImage(path);
        Size size = image.getSize();
        double aspectRatioK;
        if(size.width > size.height){
            aspectRatioK = (double)size.width / size.height;
        } else {
            aspectRatioK = (double)size.height / size.width;
        }

        if(aspectRatioK > MAX_ASPECT_RATIO_K){
            throw new AspectMaxFactorRatioException(
                    new AspectRatioData(size.width, size.height, MAX_ASPECT_RATIO_K));
        }

        String destination = path + "_temp";
        String result = image.resizeProportionallyFitMax(new Size(MAX_WIDTH, MAX_HEIGHT), destination);
        if (!result.equals(path)) {
            File file = new File(path);
            FileUtils.forceDelete(file);
            FileUtils.moveFile(new File(destination), file);
        }
    }

    @Override
    public void saveImage(long id, InputStream inputStream) {
        String path = generateImagePath(id);
        new File(path).getParentFile().mkdirs();
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            IOUtils.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
            fixDimensions(path);
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
                imageMagickExecutor.getImage(imagePath).createSquareThumbnail(thumbnailPath, size);
                return new FileInputStream(thumbnailPath);
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
    }
}
