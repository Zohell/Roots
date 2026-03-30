package com.example.roots;

public class Comment {
    private String commentId; // Naya variable Delete karne ke liye
    private String authorUid;
    private String text;
    private long timestamp;

    public Comment() {}

    public Comment(String authorUid, String text, long timestamp) {
        this.authorUid = authorUid;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}