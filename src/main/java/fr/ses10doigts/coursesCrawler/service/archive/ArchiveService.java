package fr.ses10doigts.coursesCrawler.service.archive;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    @Value("${archive.root-path}")
    private String rootPath;

    public void archive(String url, String body) {
        try {
            String hash = sha256(url);
            Path path = buildPath(hash);

            Files.createDirectories(path.getParent());

            try (OutputStream os = new GZIPOutputStream(
                    new BufferedOutputStream(
                            Files.newOutputStream(path)))) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            throw new RuntimeException("Archive write failed for url: " + url, e);
        }
    }

    public Optional<String> read(String url) {
        try {
            String hash = sha256(url);
            Path path = buildPath(hash);

            if (!Files.exists(path)) {
                return Optional.empty();
            }

            try (InputStream is = new GZIPInputStream(
                    new BufferedInputStream(
                            Files.newInputStream(path)))) {

                return Optional.of(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            throw new RuntimeException("Archive read failed for url: " + url, e);
        }
    }

    private Path buildPath(String hash) {
        return Paths.get(rootPath,
                hash.substring(0, 2),
                hash.substring(2, 4),
                hash + ".html.gz");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
