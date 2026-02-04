package log_rotating;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogRotateTest {
    private static final Logger logger = LoggerFactory.getLogger(LogRotateTest.class);
    private static final Logger audit = LoggerFactory.getLogger("audit");

    public static void main(String[] args) {
        RollingTarget app = new RollingTarget("APP", "logs/app/app.log", "logs/app/archived");
        RollingTarget error = new RollingTarget("ERROR", "logs/error/error.log", "logs/error/archived");
        RollingTarget auditLog = new RollingTarget("AUDIT", "logs/audit/audit.log", "logs/audit/archived");

        System.out.println(">>> [Log4j2] 로테이팅 및 '2초 후 이동' 테스트 시작");

        for (int i = 1; i <= 15000; i++) {
            logger.info("APP 기록 중... 번호: {}", i);
            logger.error("ERROR 기록 중... 번호: {}", i);
            audit.info("AUDIT 기록 중... 번호: {}", i);

            detectRolling(app);
            detectRolling(error);
            detectRolling(auditLog);

            // 500개마다 이동 시도
            if (i % 500 == 0) {
                moveToDeleteFolder(app);
                moveToDeleteFolder(error);
                moveToDeleteFolder(auditLog);
            }

            try { Thread.sleep(1); } catch (InterruptedException e) {}
            if (i % 1000 == 0) System.out.println(i + "개 로그 생성 완료...");
        }

        // [중요] 모든 로그 생성이 끝난 후, 마지막으로 생성된 파일들이 2초가 지나기를 기다립니다.
        System.out.println("\n>>> 로그 생성 종료. 마지막 파일 이동을 위해 3초간 대기합니다...");
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 최종 정리 (Final Move)
        moveToDeleteFolder(app);
        moveToDeleteFolder(error);
        moveToDeleteFolder(auditLog);

        System.out.println(">>> 테스트가 완전히 종료되었습니다. logs/*/deleted 폴더를 확인하세요!");
    }

    private static void detectRolling(RollingTarget target) {
        long currentSize = target.getActiveLog().length();
        long lastSize = target.getLastSize();

        if (currentSize > 0 && lastSize > 0 && currentSize < lastSize) {
            long rotationTime = System.currentTimeMillis();
            System.out.println("\n[!] " + target.getName() + " 롤링 감지됨");
            checkGzipTime(target.getArchivedDir(), rotationTime, target.getName());
        }
        target.updateLastSize(currentSize);
    }

    private static void checkGzipTime(File archivedDir, long startTime, String name) {
        long timeout = 3000;
        long startWait = System.currentTimeMillis();

        while (System.currentTimeMillis() - startWait < timeout) {
            File[] files = archivedDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".gz") && f.lastModified() >= startTime) {
                        long diff = f.lastModified() - startTime;
                        System.out.println("    => [" + name + "] 압축 즉시성 확인: " + diff + "ms");
                        return;
                    }
                }
            }
            try { Thread.sleep(50); } catch (Exception e) {}
        }
    }

    private static void moveToDeleteFolder(RollingTarget target) {
        File archivedDir = target.getArchivedDir();
        File deleteDir = new File(archivedDir.getParent(), "deleted");
        if (!deleteDir.exists()) deleteDir.mkdirs();

        File[] files = archivedDir.listFiles();
        if (files != null) {
            long now = System.currentTimeMillis();
            for (File f : files) {
                // 수정 시간 기준으로 2초(2000ms)가 지났는지 확인
                if (f.getName().endsWith(".gz") && (now - f.lastModified() > 2000)) {
                    try {
                        File dest = new File(deleteDir, f.getName());
                        Files.move(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("    [MOVE] " + target.getName() + " 보관 파일 이동됨: " + f.getName());
                    } catch (Exception e) {
                        System.err.println("    [ERROR] 이동 실패: " + e.getMessage());
                    }
                }
            }
        }
    }
}