package com.hejulian.blog.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "blog.upload")
public class UploadProperties {

    private String dir = "uploads";

    public Path resolveDirectory() {
        return Paths.get(dir).toAbsolutePath().normalize();
    }
}
