package log_rotating;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
	        () -> assertFalse(err.contains(auditMsg))   // ✅ 오타 수정
	    );

	    // audit.log: audit만 있어야 함 (additivity=false 전제)
	    assertAll("audit.log 분리",
	        () -> assertTrue(audit.contains(auditMsg)),
	        () -> assertFalse(audit.contains(infoMsg)),
	        () -> assertFalse(audit.contains(warnMsg)),
	        () -> assertFalse(audit.contains(errorMsg))
	    );
	}
}
