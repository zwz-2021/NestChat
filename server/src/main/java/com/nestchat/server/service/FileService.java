package com.nestchat.server.service;

import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.IdGenerator;
import com.nestchat.server.common.MediaUrlHelper;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.response.UploadFileResponse;
import com.nestchat.server.entity.FileRecord;
import com.nestchat.server.mapper.FileRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

@Service
public class FileService {

    @Value("${nestchat.upload.base-path}")
    private String uploadBasePath;

    private final FileRecordMapper fileRecordMapper;
    private final MediaUrlHelper mediaUrlHelper;

    public FileService(FileRecordMapper fileRecordMapper, MediaUrlHelper mediaUrlHelper) {
        this.fileRecordMapper = fileRecordMapper;
        this.mediaUrlHelper = mediaUrlHelper;
    }

    public UploadFileResponse uploadImage(MultipartFile file, String bizType, String userId) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        String fileId = IdGenerator.generate("img");
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName, "jpg");
        String mimeType = file.getContentType();

        String relativePath = "image/" + bizType + "/" + fileId + "." + ext;
        String fullPath = buildFullPath(relativePath);
        saveFile(file, fullPath);

        String thumbRelativePath = "image/" + bizType + "/" + fileId + "_thumb." + ext;
        String thumbFullPath = buildFullPath(thumbRelativePath);
        generateThumbnail(fullPath, thumbFullPath, 200);

        FileRecord record = new FileRecord();
        record.setFileId(fileId);
        record.setUserId(userId);
        record.setFileUrl(relativePath);
        record.setThumbnailUrl(thumbRelativePath);
        record.setMimeType(mimeType != null ? mimeType : "image/jpeg");
        record.setFileSize(file.getSize());
        record.setBizType(bizType);
        record.setDurationSeconds(0);
        record.setCreatedAt(LocalDateTime.now());
        fileRecordMapper.insert(record);

        UploadFileResponse resp = new UploadFileResponse();
        resp.setFileId(fileId);
        resp.setFileUrl(mediaUrlHelper.toPublicUrl(relativePath));
        resp.setThumbnailUrl(mediaUrlHelper.toPublicUrl(thumbRelativePath));
        resp.setMimeType(record.getMimeType());
        resp.setFileSize(file.getSize());
        resp.setDurationSeconds(0);
        return resp;
    }

    public UploadFileResponse uploadVoice(MultipartFile file, String bizType, String userId) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        String fileId = IdGenerator.generate("voice");
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName, "m4a");
        String mimeType = file.getContentType();

        String relativePath = "voice/" + bizType + "/" + fileId + "." + ext;
        String fullPath = buildFullPath(relativePath);
        saveFile(file, fullPath);

        FileRecord record = new FileRecord();
        record.setFileId(fileId);
        record.setUserId(userId);
        record.setFileUrl(relativePath);
        record.setThumbnailUrl("");
        record.setMimeType(mimeType != null ? mimeType : "audio/mp4");
        record.setFileSize(file.getSize());
        record.setBizType(bizType);
        record.setDurationSeconds(0);
        record.setCreatedAt(LocalDateTime.now());
        fileRecordMapper.insert(record);

        UploadFileResponse resp = new UploadFileResponse();
        resp.setFileId(fileId);
        resp.setFileUrl(mediaUrlHelper.toPublicUrl(relativePath));
        resp.setThumbnailUrl("");
        resp.setMimeType(record.getMimeType());
        resp.setFileSize(file.getSize());
        resp.setDurationSeconds(0);
        return resp;
    }

    private void saveFile(MultipartFile file, String path) {
        try {
            Path destPath = Paths.get(path).toAbsolutePath().normalize();
            Path parent = destPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }
    }

    private void generateThumbnail(String srcPath, String destPath, int targetWidth) {
        try {
            BufferedImage original = ImageIO.read(new File(srcPath));
            if (original == null) {
                return;
            }
            int origWidth = original.getWidth();
            int origHeight = original.getHeight();
            int newWidth = Math.min(targetWidth, origWidth);
            int newHeight = (int) ((double) origHeight / origWidth * newWidth);

            BufferedImage thumb = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
            g.dispose();

            File destFile = new File(destPath);
            File parentFile = destFile.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            String ext = destPath.substring(destPath.lastIndexOf('.') + 1);
            ImageIO.write(thumb, ext, destFile);
        } catch (IOException e) {
            // 缩略图失败不影响主流程
        }
    }

    private String getExtension(String filename, String defaultExt) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return defaultExt;
    }

    private String buildFullPath(String relativePath) {
        return Paths.get(uploadBasePath, relativePath.split("/")).toString();
    }
}
