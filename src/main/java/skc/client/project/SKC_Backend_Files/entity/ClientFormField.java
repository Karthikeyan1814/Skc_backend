package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;


@Entity
@Table(
	    name = "client_form_fields",
	    uniqueConstraints = {
	        @UniqueConstraint(
	            name = "uk_client_form_version_field_key",
	            columnNames = {"formversion", "field_key"}
	        )
	    }
	)

public class ClientFormField {
	  @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(name = "field_key", nullable = false)
	    private String fieldKey;


    @Column(nullable = false)
    private String fieldLabel;

    @Column(nullable = false)
    private String fieldType;

    private String placeholder;

    private boolean required;

    private boolean active;
    private Integer formversion;
 
   

	public Integer getFormversion() {
		return formversion;
	}






	public void setFormversion(Integer formversion) {
		this.formversion = formversion;
	}






	@Column(columnDefinition = "TEXT")
    private String options;
	
	
    

    public Long getId() {
		return id;
	}






	public String getFieldKey() {
		return fieldKey;
	}



	public void setFieldKey(String fieldKey) {
		this.fieldKey = fieldKey;
	}



	public String getFieldLabel() {
		return fieldLabel;
	}



	public void setFieldLabel(String fieldLabel) {
		this.fieldLabel = fieldLabel;
	}



	public String getFieldType() {
		return fieldType;
	}



	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}



	public String getPlaceholder() {
		return placeholder;
	}



	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}



	public boolean isRequired() {
		return required;
	}



	public void setRequired(boolean required) {
		this.required = required;
	}



	public boolean isActive() {
		return active;
	}



	public void setActive(boolean active) {
		this.active = active;
	}



	


	public String getOptions() {
		return options;
	}



	public void setOptions(String options) {
		this.options = options;
	}

	public ClientFormField() {
		
	}





	
}