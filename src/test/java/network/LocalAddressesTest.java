package network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class LocalAddressesTest {
    @Test // 루프백 없이 사설 IPv4 주소만, 중복 없이
    void returnsOnlySiteLocalIPv4WithoutLoopback() throws Exception {
        var addresses = LocalAddresses.siteLocalIPv4();
        for (String address : addresses) {
            assertTrue(address.matches("\\d{1,3}(\\.\\d{1,3}){3}"), address);
            InetAddress parsed = InetAddress.getByName(address);
            assertFalse(parsed.isLoopbackAddress(), address);
            assertTrue(parsed.isSiteLocalAddress(), address);
        }
        assertTrue(addresses.stream().distinct().count() == addresses.size());
    }

    @Test // 가상 어댑터 이름 판별
    void detectsVirtualAdapterNames() {
        assertTrue(LocalAddresses.looksVirtual("VMware Virtual Ethernet Adapter for VMnet8"));
        assertTrue(LocalAddresses.looksVirtual("vEthernet (WSL)"));
        assertTrue(LocalAddresses.looksVirtual("VirtualBox Host-Only Ethernet Adapter"));
        assertFalse(LocalAddresses.looksVirtual("Intel(R) Wi-Fi 6 AX201 160MHz"));
        assertFalse(LocalAddresses.looksVirtual("Realtek PCIe GbE Family Controller"));
        assertFalse(LocalAddresses.looksVirtual(null));
    }
}
