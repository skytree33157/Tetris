package menu;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

// 연결 지연 중인 참가자 슬롯을 모아 경고 문구를 만든다. EDT에서만 사용한다.
final class LatencyStatusTracker {
    private final Set<Integer> laggingSlots = new TreeSet<>();

    void markLagging(int slot) {
        laggingSlots.add(slot);
    }

    void markRecovered(int slot) {
        laggingSlots.remove(slot);
    }

    // 현재 참가자 목록에 없는 슬롯(나간 참가자)을 제거한다
    void keepOnly(List<Integer> slots) {
        laggingSlots.retainAll(slots);
    }

    // 지연 중인 슬롯이 없으면 빈 값. 참가자 번호는 로비와 같은 "P" + (slot + 1) 형식이다.
    Optional<String> message() {
        if (laggingSlots.isEmpty()) {
            return Optional.empty();
        }
        String names = laggingSlots.stream()
                .map(slot -> "P" + (slot + 1))
                .collect(Collectors.joining(", "));
        return Optional.of(names + " 연결 지연 중...");
    }
}
