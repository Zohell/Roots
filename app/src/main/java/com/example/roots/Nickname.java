package com.example.roots;

public class Nickname {
    private String targetUid;
    private String nickname;

    public Nickname() {}

    public Nickname(String targetUid, String nickname) {
        this.targetUid = targetUid;
        this.nickname = nickname;
    }

    public String getTargetUid() { return targetUid; }
    public void setTargetUid(String targetUid) { this.targetUid = targetUid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}