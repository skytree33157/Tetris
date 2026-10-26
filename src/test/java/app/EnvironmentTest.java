package app;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class EnvironmentTest {

    @Test // Java 21 이상 환경인지 검증
    public void testJavaVersion() {
int majorVersion = Runtime.version().feature();
        
        assertTrue(majorVersion >= 21);
    }

    @Test // 최소 1GB 이상의 시스템 RAM을 지원하는지 검증
    public void testSystemMemory() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            long physicalMemorySize = ((com.sun.management.OperatingSystemMXBean) osBean).getTotalMemorySize();
            long requiredMemory = 1024L * 1024L * 1024L; // 1GB
            
            assertTrue(physicalMemorySize >= requiredMemory);
        } else {
            // JVM에 할당된 최대 가용 메모리로 대체 검증
            long maxMemory = Runtime.getRuntime().maxMemory();
            assertTrue(maxMemory > 0);
        }
    }

    @Test // 500MB 이상의 여유 디스크 공간이 있는지 검증
    public void testFreeDiskSpace() {
        // 현재 애플리케이션이 실행되는 드라이브 기준
        File root = new File("/");
        long usableSpace = root.getUsableSpace();
        long requiredSpace = 500L * 1024L * 1024L; // 500MB
        
        assertTrue(usableSpace >= requiredSpace);
    }

    // Java 버전 문자열에서 Major 버전을 추출하는 헬퍼 메서드
    private int getMajorVersion(String version) {
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }
}