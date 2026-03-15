package com.unimart.api;

import com.unimart.api.dto.UploadDtos.PrepareUploadRequest;
import com.unimart.service.ApiException;
import com.unimart.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/prepare")
    public Object prepareUpload(@Valid @RequestBody PrepareUploadRequest request, @CurrentUser AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return uploadService.prepareUpload(request.contentType(), request.fileSize());
    }
}
