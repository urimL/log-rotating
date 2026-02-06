package dev.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import log_rotating.LogRotateTest;
import log_rotating.RollingTarget;

class Test01 {

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
