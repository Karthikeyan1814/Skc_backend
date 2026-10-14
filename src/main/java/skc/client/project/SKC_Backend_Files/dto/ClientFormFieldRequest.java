package skc.client.project.SKC_Backend_Files.dto;





public class ClientFormFieldRequest {

    private String fieldKey;

    private String fieldLabel;

    private String fieldType;

    private String placeholder;

    private boolean required;

    private boolean active;
    
    private  Integer formversion;
   
    private String options;

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

	
	public Integer getFormversion() {
		return formversion;
	}

	public void setFormversion(Integer formversion) {
		this.formversion = formversion;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}
    
    
}
