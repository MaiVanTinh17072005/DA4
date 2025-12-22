package dto;

/**
 * Request DTO for creating a new livestream
 */
public class CreateLivestreamRequest {
    
    private String title;
    private String description;
    
    public CreateLivestreamRequest() {}
    
    public CreateLivestreamRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "CreateLivestreamRequest{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
