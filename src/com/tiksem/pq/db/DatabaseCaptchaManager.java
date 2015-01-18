package com.tiksem.pq.db;

import com.tiksem.mysqljava.MysqlObjectMapper;
import com.tiksem.pq.data.CaptchaInfo;
import nl.captcha.Captcha;
import nl.captcha.backgrounds.FlatColorBackgroundProducer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by CM on 1/18/2015.
 */
public class DatabaseCaptchaManager implements CaptchaManager {
    private MysqlObjectMapper mapper;
    private ImageManager imageManager;

    public DatabaseCaptchaManager(MysqlObjectMapper mapper) {
        this.mapper = mapper;
        imageManager = new FileSystemImageManager("captcha", "magic") {
            @Override
            protected void fixDimensions(String path) throws IOException {

            }
        };
    }

    private InputStream createInputStream(BufferedImage image) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };
        try {
            ImageIO.write(image, "jpg", output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ByteArrayInputStream(output.toByteArray(), 0, output.size());
    }

    @Override
    public long saveCaptcha() {
        Captcha captcha = new Captcha.Builder(200, 50)
             .addText().addBackground(new FlatColorBackgroundProducer(Color.white))
             .build();

        CaptchaInfo captchaInfo = new CaptchaInfo();
        captchaInfo.setAnswer(captcha.getAnswer());
        mapper.insert(captchaInfo);
        long id = captchaInfo.getId();
        imageManager.saveImage(id, createInputStream(captcha.getImage()));

        return id;
    }

    @Override
    public InputStream getCaptchaImage(long key) {
        return imageManager.getImageById(key);
    }

    @Override
    public boolean checkCaptcha(long key, String answer) {
        CaptchaInfo captchaInfo = mapper.getObjectById(CaptchaInfo.class, key);
        if(captchaInfo == null){
            return false;
        }

        boolean result = captchaInfo.getAnswer().equalsIgnoreCase(answer);
        mapper.delete(captchaInfo);
        return result;
    }
}
