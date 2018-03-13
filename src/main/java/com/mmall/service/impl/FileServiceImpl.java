package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by Allen
 */
@Service("iFileService")
@Slf4j
public class FileServiceImpl implements IFileService {
    public String upload(MultipartFile file,String path){
        String filenName = file.getOriginalFilename();
        //扩展名
        //abc.jpg
        String fileExtendsionName = filenName.substring(filenName.lastIndexOf(".")+1);
        String uploadFileName= UUID.randomUUID().toString()+"."+fileExtendsionName;
        log.info("开始上传文件，上传文件的文件名：{}，上传的路径：{}，新文件名：{}",filenName,path,uploadFileName);
        File fileDir=new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);//赋予可写权限
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);
        try {
            file.transferTo(targetFile);
            //文件上传成功

            //将targetFile上传到我们的FTP服务器上
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //上传完之后，删除upload下面的文件
            targetFile.delete();

        } catch (IOException e) {
            log.error("文件上传异常",e);
            return null;
        }

        return  targetFile.getName();
    }
}
