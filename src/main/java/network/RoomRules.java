package network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// 방의 생존 상태, 공격 대상, 순위를 계산하는 순수 규칙. 소켓을 다루지 않는다.
public final class RoomRules {
    private final int size;
    private final boolean[] alive;
    // 탈락 순서(먼저 탈락한 슬롯이 앞). 순위 계산에 사용한다.
    private final List<Integer> eliminationOrder = new ArrayList<>();

    public RoomRules(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.size = size;
        this.alive = new boolean[size];
        for (int i = 0; i < size; i++) {
            alive[i] = true;
        }
    }

    public int size() {
        return size;
    }

    public boolean isAlive(int slot) {
        checkSlot(slot);
        return alive[slot];
    }

    public int aliveCount() {
        int count = 0;
        for (boolean value : alive) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    // 슬롯을 탈락 처리한다. 이미 탈락했으면 false를 반환하고 상태를 바꾸지 않는다.
    public boolean eliminate(int slot) {
        checkSlot(slot);
        if (!alive[slot]) {
            return false;
        }
        alive[slot] = false;
        eliminationOrder.add(slot);
        return true;
    }

    // 생존 중이며 sender가 아닌 슬롯 중 무작위 한 명을 고른다. 대상이 없으면 -1.
    public int pickTarget(int sender, Random random) {
        Objects.requireNonNull(random, "random");
        checkSlot(sender);
        List<Integer> candidates = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (slot != sender && alive[slot]) {
                candidates.add(slot);
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    // 마지막 생존자가 1등, 그 뒤로 나중에 탈락한 순서대로 순위를 매긴다. 생존자가 둘 이상이면 아직 종료 전이다.
    public List<Integer> rankingByElimination() {
        List<Integer> ranking = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (alive[slot]) {
                ranking.add(slot);
            }
        }
        for (int i = eliminationOrder.size() - 1; i >= 0; i--) {
            ranking.add(eliminationOrder.get(i));
        }
        return ranking;
    }

    // 시간 제한 종료용 순위. 생존자는 점수 높은 순(동점은 슬롯 번호순), 그다음 탈락 역순.
    public List<Integer> rankingByScore(int[] scores) {
        Objects.requireNonNull(scores, "scores");
        if (scores.length != size) {
            throw new IllegalArgumentException("scores length must equal room size");
        }
        List<Integer> survivors = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (alive[slot]) {
                survivors.add(slot);
            }
        }
        survivors.sort(Comparator.<Integer>comparingInt(slot -> scores[slot]).reversed()
                .thenComparingInt(slot -> slot));
        List<Integer> ranking = new ArrayList<>(survivors);
        for (int i = eliminationOrder.size() - 1; i >= 0; i--) {
            ranking.add(eliminationOrder.get(i));
        }
        return ranking;
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("slot out of range: " + slot);
        }
    }
}
