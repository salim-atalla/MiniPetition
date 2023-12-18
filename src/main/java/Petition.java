

import java.util.List;

public class Petition {
    private Long id;
    private String title;
    private String content;
    private List<String> signatures;
    private Long timestamp;

    // Constructors, getters, and setters

    public Petition() {
        // Default constructor for Datastore serialization
    }

    public Petition(Long id, String title, String content, List<String> signatures, Long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.signatures = signatures;
        this.timestamp = timestamp;
    }

    // Getters and setters...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<String> signatures) {
        this.signatures = signatures;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
