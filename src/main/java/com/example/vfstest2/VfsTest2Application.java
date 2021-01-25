package com.example.vfstest2;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Log4j2
@SpringBootApplication
public class VfsTest2Application implements CommandLineRunner
{
    @Value("${remotehost}")
    private String host;
    @Value("${remoteuser}")
    private String user;
    @Value("${remotepass}")
    private String password;

    @Autowired
    private FileSystemManager manager;
    @Autowired
    private FileSystemOptions fileSystemOptions;

    public static void main(String[] args)
    {
        SpringApplication.run(VfsTest2Application.class, args);
    }

    @Override
    public void run(final String... args) throws Exception
    {
        final FileObject sftpRoot = manager.resolveFile("sftp://" + user +":" + password + "@" + host + "/.", fileSystemOptions);
        final FileObject local = manager.resolveFile("/home/kerry/temp");
        for (final FileObject child : sftpRoot.getChildren()) {
            log.info("Remote file is {}", child.getName());
            if (child.getName().getBaseName().startsWith(".")) {
                continue;
            }
            if (child.isFolder()) {
                final FileContent content = child.getContent();
                final LocalDateTime modifiedDatetime = LocalDateTime.ofInstant(Instant.ofEpochMilli(content.getLastModifiedTime()), TimeZone.getDefault().toZoneId());
                log.info("Child {} is a folder last modified {}", child.getName().toString(), modifiedDatetime);
            }
            if (child.isFile()) {
                final File file = new File("/tmp/" + child.getName().getBaseName());
                final FileObject localFile = manager.resolveFile(file.getAbsolutePath());
                localFile.createFile();
                localFile.copyFrom(child, Selectors.SELECT_SELF);
                file.setLastModified(child.getContent().getLastModifiedTime());
            }
        }
        log.info("Remote file is {}", sftpRoot.getName());
        sftpRoot.close();
        manager.close();
    }
}
