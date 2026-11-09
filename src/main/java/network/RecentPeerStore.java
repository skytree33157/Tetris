package network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import storage.DataPaths;


// 최근 접속한 IP 주소 저장
public final class RecentPeerStore {
    private static final int MAX_ADDRESSES = 5; // 최대 주소 저장 공간

    private final Path file;

    public RecentPeerStore() {
        this(DataPaths.dataDir().toPath().resolve("recent-p2p-addresses.txt"));
    }

    RecentPeerStore(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    // 최근 접속한 IP 주소 불러오기
    public synchronized List<String> load() throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> addresses = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String address = line.trim();
            if (!address.isEmpty() && !addresses.contains(address)) {
                addresses.add(address);
                if (addresses.size() == MAX_ADDRESSES) {
                    break;
                }
            }
        }
        return List.copyOf(addresses);
    }

    // 주소 저장 메서드
    public synchronized void remember(String address) throws IOException {
        Objects.requireNonNull(address, "address");
        String normalized = address.trim();
        if (normalized.isEmpty() || normalized.contains("\n") || normalized.contains("\r")) {
            throw new IllegalArgumentException("Invalid server address");
        }

        List<String> addresses = new ArrayList<>();
        addresses.add(normalized);
        for (String previous : load()) {
            if (!previous.equalsIgnoreCase(normalized) && addresses.size() < MAX_ADDRESSES) {
                addresses.add(previous);
            }
        }

        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "recent-p2p-", ".tmp");
        try {
            Files.write(temporaryFile, addresses, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
