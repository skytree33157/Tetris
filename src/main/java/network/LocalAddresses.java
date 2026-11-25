package network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// 방을 만든 쪽이 상대에게 알려줄 내 IP 주소 목록
public final class LocalAddresses {
    // 가상 어댑터 이름에 자주 들어가는 단어. 실제 랜카드 주소를 먼저 보여주려고 사용
    private static final String[] VIRTUAL_HINTS = {
        "virtual", "vmware", "vbox", "hyper-v", "vethernet", "wsl", "docker", "loopback", "tap", "tun"
    };

    private LocalAddresses() {}

    /**
     * 켜져 있는 인터페이스의 사설 IPv4 주소를 반환한다. 루프백은 제외하고,
     * 가상 어댑터로 보이는 주소는 목록 뒤쪽에 둔다. 찾지 못하면 빈 목록.
     */
    public static List<String> siteLocalIPv4() {
        List<String> physical = new ArrayList<>();
        List<String> virtual = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                boolean looksVirtual = looksVirtual(nic.getDisplayName()) || looksVirtual(nic.getName());
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        String text = address.getHostAddress();
                        List<String> target = looksVirtual ? virtual : physical;
                        if (!physical.contains(text) && !virtual.contains(text)) {
                            target.add(text);
                        }
                    }
                }
            }
        } catch (SocketException | NullPointerException e) {
            // 인터페이스 목록을 못 읽는 환경이면 빈 목록
            return List.of();
        }
        physical.addAll(virtual);
        return List.copyOf(physical);
    }

    static boolean looksVirtual(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String hint : VIRTUAL_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
