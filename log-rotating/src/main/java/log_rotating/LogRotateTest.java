package log_rotating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogRotateTest {
	private static final Logger logger = LogManager.getLogger(LogRotateTest.class);
	private static final Logger audit = LogManager.getLogger("audit"); 


	public static void main(String[] args) {
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
	        	
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}

			if (i % 1000 == 0)
				System.out.println(i + "개 로그 생성...");
		}

		System.out.println(">>> 테스트 종료! logs 폴더를 새로고침(F5) 하세요.");
	}
}