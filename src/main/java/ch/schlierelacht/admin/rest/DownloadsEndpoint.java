package ch.schlierelacht.admin.rest;

import ch.schlierelacht.admin.dto.DownloadDTO;
import ch.schlierelacht.admin.service.DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/downloads")
@RequiredArgsConstructor
public class DownloadsEndpoint {

    private final DownloadService downloadService;

    @GetMapping
    public ResponseEntity<List<DownloadDTO>> getDownloads() {
        log.info("GET /api/downloads");
        return ResponseEntity.ok(downloadService.findAll());
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        log.info("GET /api/downloads/{}/file", id);
        return downloadService.findById(id)
                              .map(download -> {
                                  var headers = new HttpHeaders();
                                  headers.setContentType(MediaType.parseMediaType(download.getFiletype()));
                                  headers.setContentDisposition(ContentDisposition.attachment()
                                                                                  .filename(download.getFilename())
                                                                                  .build());
                                  return ResponseEntity.ok()
                                                       .headers(headers)
                                                       .body(download.getFileData());
                              })
                              .orElse(ResponseEntity.notFound().build());
    }
}
