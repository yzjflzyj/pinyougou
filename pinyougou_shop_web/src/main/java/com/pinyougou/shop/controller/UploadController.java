package com.pinyougou.shop.controller;

import com.pinyougou.Utils.FastDFSClient;
import com.pinyougou.entity.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {

    //读取配置文件中的fastDFS服务的地址
    @Value("${FILE_SERVER_URL}")
    private String FILE_SERVER_URL;

    @RequestMapping("upload")
    public Result upload(MultipartFile file){
        try {
            //1、图片原来的名字
            String oldName = file.getOriginalFilename();
            //2、获取后缀名，不带"."
            String extName = oldName.substring(oldName.lastIndexOf(".") + 1);
            //3、创建FastDFS客户端
            FastDFSClient fastDFSClient = new FastDFSClient("classpath:fdfs_client.conf");
            //4、上传文件到FastDFS
            String fileId = fastDFSClient.uploadFile(file.getBytes(), extName);
            //5、拼接文件url
            String url = FILE_SERVER_URL + fileId;
            //6、返回文件url
            return new Result(true, url);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "上传失败");
        }
    }

}
