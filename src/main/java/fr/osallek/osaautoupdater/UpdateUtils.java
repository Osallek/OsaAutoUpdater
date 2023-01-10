package fr.osallek.osaautoupdater;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class UpdateUtils {

    private UpdateUtils() {
    }

    public static Path getDocumentsPath() {
        AtomicReference<Path> documentsFolder = new AtomicReference<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            try {
                readRegistry("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders",
                                               "Personal")
                        .map(Path::of).ifPresent(documentsFolder::set);
            } catch (Exception ignored) {
            }
        }

        return (documentsFolder.get() == null ? Path.of(System.getProperty("user.home")) : documentsFolder.get()).resolve("Osallek");
    }

    public static Optional<String> readRegistry(String location, String key) throws InterruptedException, IOException {
        Process process = new ProcessBuilder("reg", "query", "\"" + location + "\"", "/v", "\"" + key + "\"").start();
        process.waitFor();

        String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (StringUtils.isNotBlank(text)) {
            if (text.contains("\t")) {
                return Optional.of(text.substring(text.lastIndexOf("\t")).trim());
            } else if (text.contains("    ")) {
                return Optional.of(text.substring(text.lastIndexOf("    ")).trim());
            }
        }

        return Optional.empty();
    }

    public static void unzip(Path zip, Path destination) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();

                Path newFile = newFile(destination, zipEntry);
                if (zipEntry.isDirectory()) {
                    FileUtils.forceMkdir(newFile.toFile());
                } else {
                    // fix for Windows-created archives
                    FileUtils.forceMkdir(newFile.getParent().toFile());
                    copyToFile(zipFile.getInputStream(zipEntry), newFile.toFile());
                }
            }
        }
    }

    public static Path newFile(Path destinationDir, ZipEntry zipEntry) throws IOException {
        Path destFile = destinationDir.resolve(zipEntry.getName());

        Path destDirPath = destinationDir.toFile().getCanonicalFile().toPath();
        Path destFilePath = destFile.toFile().getCanonicalFile().toPath();

        if (!destFilePath.startsWith(destDirPath)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void copyToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1_000_000];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }
}
