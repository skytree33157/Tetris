package network;

// 대기실 참가자 한 명의 표시 정보
public record RoomMember(String name, boolean ready, boolean host) {
}
