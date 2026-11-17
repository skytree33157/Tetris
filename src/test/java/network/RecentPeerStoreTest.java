package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentPeerStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void remembersMostRecentUniqueAddressesUpToLimit() throws Exception {
        RecentPeerStore store = new RecentPeerStore(
                temporaryDirectory.resolve("recent-addresses.txt"));

        for (int address = 1; address <= 6; address++) {
            store.remember("192.168.0." + address);
        }
        store.remember("192.168.0.4");

        assertEquals(List.of(
                "192.168.0.4",
                "192.168.0.6",
                "192.168.0.5",
                "192.168.0.3",
                "192.168.0.2"), store.load());
    }

    @Test
    void missingHistoryFileMeansNoRecentAddresses() throws Exception {
        RecentPeerStore store = new RecentPeerStore(
                temporaryDirectory.resolve("missing.txt"));

        assertEquals(List.of(), store.load());
        assertFalse(java.nio.file.Files.exists(temporaryDirectory.resolve("missing.txt")));
    }
}
