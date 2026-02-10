package log_rotating;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogRotateTestTest {

    @Test
    @DisplayName("detectRolling 메서드는 로그 파일 크기가 감소하면 롤링을 감지한다")
    void test() throws Exception {

        PrintStream originalOut = System.out; // 테스트 끝나고 복구용

        try {
            // Given
            File tempLog = File.createTempFile("app", ".log");
            tempLog.deleteOnExit();

            // archivedDir은 존재하는 디렉터리여야 listFiles()가 안전하게 동작함
            File archivedDir = Files.createTempDirectory("archived").toFile();
            archivedDir.deleteOnExit();

            // 1) 현재 파일에 내용 기록(크기 > 0 만들기)
            Files.write(tempLog.toPath(), "크기가 0보다 큰 로그 데이터입니다.".getBytes());

            RollingTarget target = new RollingTarget(
                    "APP",
                    tempLog.getAbsolutePath(),
                    archivedDir.getAbsolutePath()
            );

            // 2) lastSize를 현재보다 크게 세팅해서 (currentSize < lastSize) 조건 만들기
            target.updateLastSize(10_000);

            // 3) System.out 캡처
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            // private static detectRolling() 메서드를 reflection으로 호출 준비
            Method detectRolling = LogRotateTest.class.getDeclaredMethod("detectRolling", RollingTarget.class);
            detectRolling.setAccessible(true);

            // When
            detectRolling.invoke(null, target);

            // Then
            String output = out.toString();
            assertTrue(output.contains("롤링 감지됨"),
                    "로그 파일 크기가 감소(currentSize < lastSize)하면 롤링 감지 로그가 출력되어야 한다");

        } finally {
            // System.out 원복 (다른 테스트에 영향 방지)
            System.setOut(originalOut);
        }
    }
    
    @Test
	@DisplayName("INFO/WARN/ERROR 로그가 들어오면 각각의 레벨에 맞는 .log 파일이 생성된다.")
	void levelSeparatedFile() throws Exception {
		// Given
		Logger logger = LoggerFactory.getLogger(LogRotateTestTest.class);
		Logger audit = LoggerFactory.getLogger("audit");
		
		Path appLog = Paths.get("logs/app/app.log");
		Path errorLog = Paths.get("logs/error/error.log");
		Path auditLog = Paths.get("logs/audit/audit.log");

		Files.createDirectories(appLog.getParent());
		Files.createDirectories(errorLog.getParent());
		Files.createDirectories(auditLog.getParent());
	

		// When
		logger.info("INFO"); 
		logger.error("ERROR");
		audit.info("AUDIT");
		
		Thread.sleep(500);
		
		// then
		assertAll("파일 생성", () -> assertTrue(Files.exists(appLog), "app.log 파일이 생성되어야 한다"),
				() -> assertTrue(Files.exists(errorLog), "error.log 파일이 생성되어야 한다"),
				() -> assertTrue(Files.exists(auditLog), "audit.log 파일이 생성되어야 한다."));

	}

	@Test
	@DisplayName("INFO/WARN/ERROR 로그가 레벨 필터 정책에 따라 app.log, error.log로 분리 기록된다")
	void levelSeparatedToCorrectFiles() throws Exception {
	    // Given
	    Logger logger = LoggerFactory.getLogger(LogRotateTestTest.class);
	    Logger auditLogger = LoggerFactory.getLogger("audit");

	    Path appLog = Paths.get("logs/app/app.log");
	    Path errorLog = Paths.get("logs/error/error.log");
	    Path auditLog = Paths.get("logs/audit/audit.log");

	    Files.createDirectories(appLog.getParent());
	    Files.createDirectories(errorLog.getParent());
	    Files.createDirectories(auditLog.getParent());

	    Files.writeString(appLog, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	    Files.writeString(errorLog, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	    Files.writeString(auditLog, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

	    String id = String.valueOf(System.currentTimeMillis());
	    String infoMsg  = "INFO_" + id;
	    String warnMsg  = "WARN_" + id;
	    String errorMsg = "ERROR_" + id;
	    String auditMsg = "AUDIT_" + id;

	    // When
	    logger.info(infoMsg);
	    logger.warn(warnMsg);
	    logger.error(errorMsg);
	    auditLogger.info(auditMsg);

	    Thread.sleep(600);

	    // Then - 파일 생성 확인
	   
	    String app = Files.readString(appLog);
	    String err = Files.readString(errorLog);
	    String audit = Files.readString(auditLog);

	    // app.log: WARN 이상은 없어야 함 (INFO는 있어야 함)
	    assertAll("app.log 필터",
	        () -> assertTrue(app.contains(infoMsg)),
	        () -> assertFalse(app.contains(warnMsg)),
	        () -> assertFalse(app.contains(errorMsg)),
	        () -> assertFalse(app.contains(auditMsg))
	    );

	    // error.log: WARN 이상만 있어야 함
	    assertAll("error.log 필터",
	        () -> assertFalse(err.contains(infoMsg)),
	        () -> assertTrue(err.contains(warnMsg)),
	        () -> assertTrue(err.contains(errorMsg)),
	        () -> assertFalse(err.contains(auditMsg))
	    );

	    // audit.log: audit만 있어야 함 (additivity=false 전제)
	    assertAll("audit.log 분리",
	        () -> assertTrue(audit.contains(auditMsg)),
	        () -> assertFalse(audit.contains(infoMsg)),
	        () -> assertFalse(audit.contains(warnMsg)),
	        () -> assertFalse(audit.contains(errorMsg))
	    );
	}
	
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
    
	@TempDir
	File tempDir;

	@Test
	@DisplayName("2초가 지난 후 archived -> deleted로 파일이 이동되어야 한다")
	// Files.write() 메소드는 내부적으로 IOException을 발생시킬 수 있는 메소드이기 때문에 예외처리 추가
	void test1() throws Exception {

		// --- given ---
		File archivedDir = new File(tempDir, "archived");
		archivedDir.mkdirs();

		File deletedDir = new File(tempDir, "deleted");
		
		File gzFile = new File(archivedDir, "test.log.gz");
		
		Files.write(gzFile.toPath(), "test".getBytes());

		// move 조건이 '2초 초과'이므로 확실한 이동 대상이 되도록, 3초 전에 만들어진 파일로 보이게끔 수정시간 변경
		gzFile.setLastModified(System.currentTimeMillis() - 3000);

		RollingTarget target = new RollingTarget("TEST", "", archivedDir.getAbsolutePath());

		
		// --- when ---
		LogRotateTest.moveToDeleteFolder(target);

		
		// --- then ---
		File movedFile = new File(deletedDir, "test.log.gz");

		assertAll(() -> {
			assertFalse(gzFile.exists());
			assertTrue(movedFile.exists());
		});
	}
}
