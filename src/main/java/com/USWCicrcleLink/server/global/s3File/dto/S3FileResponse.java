package com.USWCicrcleLink.server.global.s3File.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3FileResponse {
    private String presignedUrl;
    private String s3FileName;
}
