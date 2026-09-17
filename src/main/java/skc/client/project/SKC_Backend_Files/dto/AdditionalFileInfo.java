package skc.client.project.SKC_Backend_Files.dto;

public class AdditionalFileInfo {

    private Long id;
    private String category;
    private String role;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public AdditionalFileInfo() {
        super();
    }

    public AdditionalFileInfo(Long id, String category, String role) {
        super();
        this.id = id;
        this.category = category;
        this.role = role;
    }
}