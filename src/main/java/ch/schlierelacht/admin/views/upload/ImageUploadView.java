package ch.schlierelacht.admin.views.upload;

import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@PageTitle("Image Upload")
@Route(value = "upload", layout = MainLayout.class)
@PermitAll
public class ImageUploadView extends VerticalLayout {

    private final CloudflareService cloudflareService;
    private final Div imageContainer;

    public ImageUploadView(CloudflareService cloudflareService) {
        this.cloudflareService = cloudflareService;

        add(new H2("Cloudflare Image Upload"));

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFiles(1);

        imageContainer = new Div();
        imageContainer.setWidthFull();

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            String contentType = event.getMIMEType();
            long contentLength = event.getContentLength();
            InputStream inputStream = buffer.getInputStream();

            try {
                byte[] bytes = inputStream.readAllBytes();
                MultipartFile multipartFile = new CustomMultipartFile(bytes, fileName, contentType);

                String id = UUID.randomUUID().toString();
                String creator = "AdminUI"; // Or get from security context if needed

                cloudflareService.upload(multipartFile, id, creator).ifPresentOrElse(cloudflareId -> {
                    Notification.show("Upload successful! ID: " + cloudflareId);
                    displayUploadedImage(cloudflareId);
                }, () -> {
                    Notification.show("Upload failed", 3000, Notification.Position.MIDDLE);
                });

            } catch (IOException e) {
                Notification.show("Error reading uploaded file: " + e.getMessage());
            }
        });

        add(upload, imageContainer);
    }

    private void displayUploadedImage(String cloudflareId) {
        // This is just a placeholder to show something was uploaded
        // In a real scenario, you'd use the cloudflareImagedeliveryUrl
        Notification.show("Image uploaded with Cloudflare ID: " + cloudflareId);
    }

    private static class CustomMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public CustomMultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
