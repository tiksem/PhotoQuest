package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Stored;
import com.tiksem.mysqljava.annotations.Table;

/**
 * Created by CM on 1/18/2015.
 */
@Table
public class CaptchaInfo {
    @PrimaryKey
    private Long id;
    @Stored
    private String answer;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
