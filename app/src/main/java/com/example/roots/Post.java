package com.example.roots;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.List;

public class Post {
    @DocumentId
    private String postId; // Firebase automatically isme Post ki ID daal dega!

    private String authorUid;
    private int commentCount = 0;
    private List<String> likes = new ArrayList<>(); // Ek hi baar declare kiya hai
    private String imageUrl;
    private String caption;
    private List<String> visibleTo;
    private String mediaType;
    private long timestamp;
    private List<String> authorFeeds = new ArrayList<>();

    public Post() {} // Empty constructor for Firestore

    public Post(String authorUid, String imageUrl, String caption, List<String> visibleTo, String mediaType, long timestamp, List<String> likes, int commentCount) {
        this.authorUid = authorUid;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.visibleTo = visibleTo;
        this.mediaType = mediaType;
        this.timestamp = timestamp;
        this.likes = likes != null ? likes : new ArrayList<>();
        this.commentCount = commentCount;
    }
    public List<String> getAuthorFeeds() { return authorFeeds; }
    public void setAuthorFeeds(List<String> authorFeeds) { this.authorFeeds = authorFeeds; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public List<String> getVisibleTo() { return visibleTo; }
    public void setVisibleTo(List<String> visibleTo) { this.visibleTo = visibleTo; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getLikes() { return likes; }
    public void setLikes(List<String> likes) { this.likes = likes; }
}