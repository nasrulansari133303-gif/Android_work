package com.example.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public final class FileUploadUtils {

    private FileUploadUtils() {}

    public static MultipartBody.Part uriToMultipart(Context context, Uri uri, String partName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (ext == null || ext.isEmpty()) {
            ext = "jpg";
        }
        byte[] bytes;
        try (InputStream is = resolver.openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (is == null) {
                throw new IOException("Cannot read selected file");
            }
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            bytes = buffer.toByteArray();
        }
        RequestBody reqBody = RequestBody.create(bytes, MediaType.parse(mimeType));
        return MultipartBody.Part.createFormData(partName, "upload_" + System.currentTimeMillis() + "." + ext, reqBody);
    }
}
