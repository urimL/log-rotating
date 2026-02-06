package log_rotating;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogRotateTestTest {

    @AfterEach
    void tearDown() {
        LogManager.shutdown();
    }

    @Test
    @DisplayName("50KB 초과 시 archived 폴더에 .gz 파일이 생성되는지 확인")
    void testGzipFileExists(@TempDir Path tempDir) throws InterruptedException {
        System.setProperty("LOG_DIR", tempDir.toString());
        
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.reconfigure();
        
        Logger logger = LoggerFactory.getLogger(LogRotateTestTest.class);
        
        File archivedDir = tempDir.resolve("app/archived").toFile();

        for (int i = 0; i < 10000; i++) {
            logger.info("롤링 테스트 데이터 기록 중... 번호: " + i);
        }

        Thread.sleep(2500); 

        File[] files = archivedDir.listFiles((dir, name) -> name.endsWith(".gz"));

        assertNotNull(files, "archived 폴더가 생성되지 않았습니다.");
        assertTrue(files.length > 0, "실제 .gz 압축 파일이 생성되지 않았습니다.");
        
        System.out.println("드디어 성공! 생성 경로: " + archivedDir.getAbsolutePath());
        System.out.println("생성된 파일: " + files[0].getName());
    }
}