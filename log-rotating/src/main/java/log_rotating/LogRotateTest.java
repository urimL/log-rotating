package log_rotating;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogRotateTest {
    private static final Logger logger = LogManager.getLogger(LogRotateTest.class);
    private static final Logger audit = LogManager.getLogger("audit"); 

    public static void main(String[] args) {
    	File logFile = new File("logs/app.log");
    	long lastSize = 0;
    	long rotationStartTime = 0;
    	
        System.out.println(">>> [Log4j2] 로테이팅 테스트 시작");
        
        for (int i = 1; i <= 8000; i++) {
			if (i % 4 == 1)
				logger.info("info 데이터 기록 중... 번호: {} | 256KB 롤링 및 2개 유지 정책 검증", i);
			else if (i % 4 == 2)
				logger.error("error 데이터 기록 중.. 번호: {} | 256KB 롤링 및 2개 유지 정책 검증", i);
			else if (i % 4 == 3)
				logger.warn("warn 데이터 기록 중.. 번호: {} | 256KB 롤링 및 2개 유지 정책 검증", i);
			else
				audit.info("AUDIT: login userId=kim ip=1.2.3.4 result=SUCCESS");
	        	audit.info("AUDIT: permission-change userId=admin target=kim role=USER->ADMIN");
        }

        for (int i = 1; i <= 15000; i++) {
            logger.info("압축 검증 데이터 기록 중... 번호: {} | 현재 크기: {} bytes", i, logFile.length());
            
            long currentSize = logFile.length();
            
            if (currentSize < lastSize && lastSize > 0) {
            	rotationStartTime = System.currentTimeMillis();
            	System.out.println("\n[!] 롤링 포착: " + rotationStartTime);
            	
            	checkGzipTime(rotationStartTime);
            }
            
            lastSize = currentSize;
            
            try { Thread.sleep(1); } catch (InterruptedException e) {}
            
            if (i % 1000 == 0) System.out.println(i + "개 로그 생성...");
        }

        System.out.println(">>> 테스트 종료! logs 폴더를 새로고침(F5) 하세요.");
    }
    
    private static void checkGzipTime(long startTime) {
        File archivedDir = new File("logs/archived");
        
        long timeout = 3000; // 최대 3초 대기
        long startWait = System.currentTimeMillis();

        while (System.currentTimeMillis() - startWait < timeout) {
            File[] files = archivedDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    // 롤링 시점 이후 생성된 .gz 파일 확인
                    if (f.getName().endsWith(".gz") && f.lastModified() >= startTime) {
                        long diff = f.lastModified() - startTime;
                        System.out.println("\n==== 즉시성 검증 결과 ====");
                        System.out.println("압축 파일: " + f.getName());
                        System.out.println("소요 시간: " + diff + "ms");
                        System.out.println("결과: " + (diff <= 1000 ? "PASS (즉시성 확인)" : "FAIL"));
                        return;
                    }
                }
            }
            try { Thread.sleep(50); } catch (Exception e) {}
        }
    }
}