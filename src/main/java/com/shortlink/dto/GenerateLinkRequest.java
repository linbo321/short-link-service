package com.shortlink.dto;

public class GenerateLinkRequest {
    private String url;
    private Integer expireHours;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(Integer expireHours) {
        this.expireHours = expireHours;
    }
}
