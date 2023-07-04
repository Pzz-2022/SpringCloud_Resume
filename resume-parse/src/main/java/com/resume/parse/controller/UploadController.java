package com.resume.parse.controller;

import com.resume.base.model.RestResponse;
import com.resume.parse.dto.FileChunkDTO;
import com.resume.parse.utils.UploadUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/*
 *@filename: SingleUploadController
 *@author: lyh
 *@date:2023/7/4 16:33
 *@version 1.0
 *@description TODO
 */
@RestController
@Api(tags = "简历上传接口")
public class UploadController {

    @Autowired
    private UploadUtil uploadService;

    @ApiOperation(value = "单个简历上传")
    @PostMapping("/upload-single-resume")
    public RestResponse<String> uploadSingleResume(@RequestParam("file") MultipartFile file) throws IOException {
        //获取原始文件名
        String originalFilename = file.getOriginalFilename();
        //取文件后缀
        String fileSuffix=originalFilename.substring(originalFilename.lastIndexOf('.'));
        //创建新的文件名称
        String fileURL=uploadService.uploadByBytes(file.getBytes(), UUID.randomUUID()+fileSuffix);
        return RestResponse.success(fileURL);
    }

//    @ApiOperation(value = "判断简历文件是否重复")
//    @GetMapping("/check-chunk")
//    public RestResponse<String> checkChunkExist(@RequestBody FileChunkDTO chunkDTO){
//
//    }
}
